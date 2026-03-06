package com.exemple.backend.service;

import com.exemple.backend.entity.Beneficio;
import com.exemple.backend.repository.BeneficioRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficioServiceTest {

    @Mock
    private BeneficioRepository beneficioRepository;

    @InjectMocks
    private BeneficioService beneficioService;

    private Beneficio beneficioOrigem;
    private Beneficio beneficioDestino;
    private final Long ID_ORIGEM = 1L;
    private final Long ID_DESTINO = 2L;
    private final BigDecimal VALOR_TRANSFERENCIA = new BigDecimal("100.00");
    private final BigDecimal SALDO_INICIAL_ORIGEM = new BigDecimal("500.00");
    private final BigDecimal SALDO_INICIAL_DESTINO = new BigDecimal("200.00");

    @BeforeEach
    void setUp() {
        beneficioOrigem = new Beneficio();
        beneficioOrigem.setId(ID_ORIGEM);
        beneficioOrigem.setValor(SALDO_INICIAL_ORIGEM);
        beneficioOrigem.setAtivo(true);
        beneficioOrigem.setVersion(1L);

        beneficioDestino = new Beneficio();
        beneficioDestino.setId(ID_DESTINO);
        beneficioDestino.setValor(SALDO_INICIAL_DESTINO);
        beneficioDestino.setAtivo(true);
        beneficioDestino.setVersion(1L);
    }

    // Testes para transferência com Optimistic Locking
    @Test
    void transfer_DeveRealizarTransferenciaComSucesso() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));
        when(beneficioRepository.findById(ID_DESTINO)).thenReturn(Optional.of(beneficioDestino));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        assertDoesNotThrow(() -> beneficioService.transfer(ID_ORIGEM, ID_DESTINO, VALOR_TRANSFERENCIA));

        // Assert
        assertEquals(new BigDecimal("400.00"), beneficioOrigem.getValor());
        assertEquals(new BigDecimal("300.00"), beneficioDestino.getValor());
        
        verify(beneficioRepository, times(1)).findById(ID_ORIGEM);
        verify(beneficioRepository, times(1)).findById(ID_DESTINO);
        verify(beneficioRepository, times(2)).save(any(Beneficio.class));
    }

    @Test
    void transfer_DeveLancarExcecaoQuandoBeneficioOrigemNaoEncontrado() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.empty());
        when(beneficioRepository.findById(ID_DESTINO)).thenReturn(Optional.of(beneficioDestino));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> beneficioService.transfer(ID_ORIGEM, ID_DESTINO, VALOR_TRANSFERENCIA));

        assertTrue(exception.getMessage().contains("Benefício de origem não encontrado"));
    }

    // Testes para transferência com Pessimistic Locking
    @Test
    void transferWithPessimisticLock_DeveRealizarTransferenciaComSucesso() {
        // Arrange
        when(beneficioRepository.findByIdWithPessimisticLock(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));
        when(beneficioRepository.findByIdWithPessimisticLock(ID_DESTINO)).thenReturn(Optional.of(beneficioDestino));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        assertDoesNotThrow(() -> beneficioService.transferWithPessimisticLock(ID_ORIGEM, ID_DESTINO, VALOR_TRANSFERENCIA));

        // Assert
        assertEquals(new BigDecimal("400.00"), beneficioOrigem.getValor());
        assertEquals(new BigDecimal("300.00"), beneficioDestino.getValor());
    }

    // Testes para transferência com Mixed Locking
    @Test
    void transferWithMixedLock_DeveRealizarTransferenciaComSucesso() {
        // Arrange
        when(beneficioRepository.findByIdWithPessimisticLock(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));
        when(beneficioRepository.findById(ID_DESTINO)).thenReturn(Optional.of(beneficioDestino));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        assertDoesNotThrow(() -> beneficioService.transferWithMixedLock(ID_ORIGEM, ID_DESTINO, VALOR_TRANSFERENCIA));

        // Assert
        assertEquals(new BigDecimal("400.00"), beneficioOrigem.getValor());
        assertEquals(new BigDecimal("300.00"), beneficioDestino.getValor());
    }

    

    // Testes para validações de parâmetros
    @Test
    void validarParametrosTransferencia_DeveLancarExcecaoParaParametrosNulos() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(null, ID_DESTINO, VALOR_TRANSFERENCIA));
        
        assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(ID_ORIGEM, null, VALOR_TRANSFERENCIA));
            
        assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(ID_ORIGEM, ID_DESTINO, null));
    }

    @Test
    void validarParametrosTransferencia_DeveLancarExcecaoParaMesmoBeneficio() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(ID_ORIGEM, ID_ORIGEM, VALOR_TRANSFERENCIA));

        assertTrue(exception.getMessage().contains("mesmo benefício"));
    }

    @Test
    void validarParametrosTransferencia_DeveLancarExcecaoParaValorZeroOuNegativo() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(ID_ORIGEM, ID_DESTINO, BigDecimal.ZERO));
            
        assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(ID_ORIGEM, ID_DESTINO, new BigDecimal("-100.00")));
    }

    @Test
    void validarParametrosTransferencia_DeveLancarExcecaoParaValorAcimaLimite() {
        // Arrange
        BigDecimal valorAcimaLimite = new BigDecimal("1000001.00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> beneficioService.transfer(ID_ORIGEM, ID_DESTINO, valorAcimaLimite));

        assertTrue(exception.getMessage().contains("limite permitido"));
    }

    // Testes para métodos auxiliares
    @Test
    void consultarSaldo_DeveRetornarSaldoCorretamente() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        BigDecimal saldo = beneficioService.consultarSaldo(ID_ORIGEM);

        // Assert
        assertEquals(SALDO_INICIAL_ORIGEM, saldo);
    }

    @Test
    void isTransferenciaPossivel_DeveRetornarTrueParaTransferenciaValida() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        boolean resultado = beneficioService.isTransferenciaPossivel(ID_ORIGEM, VALOR_TRANSFERENCIA);

        // Assert
        assertTrue(resultado);
    }

    @Test
    void isTransferenciaPossivel_DeveRetornarFalseParaSaldoInsuficiente() {
        // Arrange
        beneficioOrigem.setValor(new BigDecimal("50.00"));
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        boolean resultado = beneficioService.isTransferenciaPossivel(ID_ORIGEM, VALOR_TRANSFERENCIA);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void isTransferenciaPossivel_DeveRetornarFalseParaBeneficioInativo() {
        // Arrange
        beneficioOrigem.setAtivo(false);
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        boolean resultado = beneficioService.isTransferenciaPossivel(ID_ORIGEM, VALOR_TRANSFERENCIA);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void verificarConflitoVersao_DeveRetornarTrueQuandoVersaoDiferente() {
        // Arrange
        Long versaoAtual = 2L; // Diferente da versão do benefício (1L)
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        boolean resultado = beneficioService.verificarConflitoVersao(ID_ORIGEM, versaoAtual);

        // Assert
        assertTrue(resultado);
    }

    @Test
    void verificarConflitoVersao_DeveRetornarFalseQuandoVersaoIgual() {
        // Arrange
        Long versaoAtual = 1L; // Igual à versão do benefício
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        boolean resultado = beneficioService.verificarConflitoVersao(ID_ORIGEM, versaoAtual);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void obterVersaoAtual_DeveRetornarVersaoCorreta() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        Long versao = beneficioService.obterVersaoAtual(ID_ORIGEM);

        // Assert
        assertEquals(1L, versao);
    }

    // Testes para métodos CRUD
    @Test
    void criarBeneficio_DeveSalvarBeneficioComSucesso() {
        // Arrange
        when(beneficioRepository.save(beneficioOrigem)).thenReturn(beneficioOrigem);

        // Act
        Beneficio resultado = beneficioService.criarBeneficio(beneficioOrigem);

        // Assert
        assertNotNull(resultado);
        assertEquals(beneficioOrigem, resultado);
        verify(beneficioRepository, times(1)).save(beneficioOrigem);
    }

    @Test
    void criarBeneficio_DeveLancarExcecaoParaBeneficioNulo() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> beneficioService.criarBeneficio(null));
    }

    @Test
    void buscarPorId_DeveRetornarBeneficioQuandoExistir() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.of(beneficioOrigem));

        // Act
        Optional<Beneficio> resultado = beneficioService.buscarPorId(ID_ORIGEM);

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals(beneficioOrigem, resultado.get());
    }

    @Test
    void buscarPorId_DeveRetornarVazioQuandoNaoExistir() {
        // Arrange
        when(beneficioRepository.findById(ID_ORIGEM)).thenReturn(Optional.empty());

        // Act
        Optional<Beneficio> resultado = beneficioService.buscarPorId(ID_ORIGEM);

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
