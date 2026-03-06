package com.exemple.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.exemple.backend.entity.Beneficio;
import com.exemple.backend.repository.BeneficioRepository;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class BeneficioService {

    @Autowired
    private BeneficioRepository beneficioRepository;

    /**
     * Método de transferência com PESSIMISTIC LOCKING
     * Usa lock pessimista para evitar concorrência em ambientes de alta contenção
     */
    @Transactional(rollbackFor = {Exception.class})
    public void transferWithPessimisticLock(Long fromId, Long toId, BigDecimal amount) {
        // Validações iniciais
        validarParametrosTransferencia(fromId, toId, amount);

        try {
            // Busca os benefícios com PESSIMISTIC_WRITE lock
            // Isso bloqueia as linhas no banco até o commit da transação
            Optional<Beneficio> fromOpt = beneficioRepository.findByIdWithPessimisticLock(fromId);
            Optional<Beneficio> toOpt = beneficioRepository.findByIdWithPessimisticLock(toId);
            
            validarBeneficiosEncontrados(fromOpt, toOpt, fromId, toId);
            
            Beneficio from = fromOpt.get();
            Beneficio to = toOpt.get();
            
            validarBeneficiosParaTransferencia(from, to, amount);
            
            // Executa a transferência
            realizarTransferencia(from, to, amount);
            
            // Save é opcional com @Transactional, mas explícito para clareza
            beneficioRepository.save(from);
            beneficioRepository.save(to);
            
        } catch (Exception e) {
            // @Transactional(rollbackFor = Exception.class) garante rollback automático
            throw new RuntimeException("Falha na transferência com locking pessimista: " + e.getMessage(), e);
        }
    }

    /**
     * Método de transferência com OPTIMISTIC LOCKING (Padrão com @Version)
     * Mais performático para ambientes com média/baixa contenção
     */
    @Transactional(rollbackFor = {OptimisticLockException.class, Exception.class})
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // Validações iniciais
        validarParametrosTransferencia(fromId, toId, amount);

        // Número máximo de tentativas para optimistic locking
        int maxTentativas = 3;
        int tentativa = 0;
        
        while (tentativa < maxTentativas) {
            try {
                tentativa++;
                
                // Busca os benefícios (sem lock explícito - usa optimistic locking via @Version)
                Optional<Beneficio> fromOpt = beneficioRepository.findById(fromId);
                Optional<Beneficio> toOpt = beneficioRepository.findById(toId);
                
                validarBeneficiosEncontrados(fromOpt, toOpt, fromId, toId);
                
                Beneficio from = fromOpt.get();
                Beneficio to = toOpt.get();
                
                validarBeneficiosParaTransferencia(from, to, amount);
                
                // Executa a transferência
                realizarTransferencia(from, to, amount);
                
                // Save atualizará a versão automaticamente devido à anotação @Version
                beneficioRepository.save(from);
                beneficioRepository.save(to);
                
                // Sucesso - sai do loop
                return;
                
            } catch (OptimisticLockException e) {
                // Conflito de versão - outra transação modificou os dados
                if (tentativa >= maxTentativas) {
                    throw new RuntimeException(
                        "Falha na transferência após " + maxTentativas + 
                        " tentativas devido a conflitos de concorrência. Tente novamente.", e);
                }
                
                // Aguarda um pouco antes de tentar novamente (exponential backoff)
                try {
                    Thread.sleep(100 * tentativa); // 100ms, 200ms, 300ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Transferência interrompida", ie);
                }
                
            } catch (Exception e) {
                throw new RuntimeException("Falha na transferência: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Método de transferência com LOCKING MISTO
     * Usa lock pessimista apenas no benefício de origem (mais comum ter contenção)
     */
    @Transactional(rollbackFor = {Exception.class})
    public void transferWithMixedLock(Long fromId, Long toId, BigDecimal amount) {
        // Validações iniciais
        validarParametrosTransferencia(fromId, toId, amount);

        try {
            // Lock pessimista apenas no benefício de origem (onde há mais contenção)
            Optional<Beneficio> fromOpt = beneficioRepository.findByIdWithPessimisticLock(fromId);
            // Lock otimista no benefício de destino
            Optional<Beneficio> toOpt = beneficioRepository.findById(toId);
            
            validarBeneficiosEncontrados(fromOpt, toOpt, fromId, toId);
            
            Beneficio from = fromOpt.get();
            Beneficio to = toOpt.get();
            
            validarBeneficiosParaTransferencia(from, to, amount);
            
            // Executa a transferência
            realizarTransferencia(from, to, amount);
            
            beneficioRepository.save(from);
            beneficioRepository.save(to);
            
        } catch (OptimisticLockException e) {
            throw new RuntimeException(
                "Conflito de concorrência no benefício de destino. Tente novamente.", e);
        } catch (Exception e) {
            throw new RuntimeException("Falha na transferência com locking misto: " + e.getMessage(), e);
        }
    }

    // MÉTODOS AUXILIARES PRIVADOS

    private void validarParametrosTransferencia(Long fromId, Long toId, BigDecimal amount) {
        if (fromId == null || toId == null || amount == null) {
            throw new IllegalArgumentException("Parâmetros não podem ser nulos");
        }
        
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Não é possível transferir para o mesmo benefício");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transferência deve ser positivo");
        }
        
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Valor da transferência excede o limite permitido de 1.000.000");
        }
    }

    private void validarBeneficiosEncontrados(Optional<Beneficio> fromOpt, Optional<Beneficio> toOpt, 
                                            Long fromId, Long toId) {
        if (fromOpt.isEmpty()) {
            throw new IllegalArgumentException("Benefício de origem não encontrado: " + fromId);
        }
        if (toOpt.isEmpty()) {
            throw new IllegalArgumentException("Benefício de destino não encontrado: " + toId);
        }
    }

    private void validarBeneficiosParaTransferencia(Beneficio from, Beneficio to, BigDecimal amount) {
        if (!from.getAtivo()) {
            throw new IllegalStateException("Benefício de origem não está ativo");
        }
        
        if (!to.getAtivo()) {
            throw new IllegalStateException("Benefício de destino não está ativo");
        }
        
        if (from.getValor().compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Saldo insuficiente. Saldo atual: %.2f, Valor solicitado: %.2f", 
                            from.getValor(), amount)
            );
        }
    }

    private void realizarTransferencia(Beneficio from, Beneficio to, BigDecimal amount) {
        // Realiza as operações matemáticas
        from.setValor(from.getValor().subtract(amount));
        to.setValor(to.getValor().add(amount));
        
        // A anotação @Version na entidade garante que a versão será incrementada
        // automaticamente quando a entidade for persistida
    }

    // MÉTODOS ADICIONAIS PARA CONTROLE DE CONCORRÊNCIA

    /**
     * Verifica se há conflito de versão antes da transferência
     */
    public boolean verificarConflitoVersao(Long beneficioId, Long versaoAtual) {
        Optional<Beneficio> beneficioOpt = beneficioRepository.findById(beneficioId);
        if (beneficioOpt.isEmpty()) {
            return true; // Considera como conflito se não existir
        }
        
        Beneficio beneficio = beneficioOpt.get();
        return !beneficio.getVersion().equals(versaoAtual);
    }

    /**
     * Obtém a versão atual de um benefício
     */
    public Long obterVersaoAtual(Long beneficioId) {
        Optional<Beneficio> beneficioOpt = beneficioRepository.findById(beneficioId);
        if (beneficioOpt.isEmpty()) {
            throw new IllegalArgumentException("Benefício não encontrado: " + beneficioId);
        }
        return beneficioOpt.get().getVersion();
    }

    // Método para consulta segura
    public BigDecimal consultarSaldo(Long beneficioId) {
        if (beneficioId == null) {
            throw new IllegalArgumentException("ID do benefício não pode ser nulo");
        }
        
        Optional<Beneficio> beneficioOpt = beneficioRepository.findById(beneficioId);
        if (beneficioOpt.isEmpty()) {
            throw new IllegalArgumentException("Benefício não encontrado: " + beneficioId);
        }
        
        return beneficioOpt.get().getValor();
    }
    
    // Método para verificar se transferência é possível
    public boolean isTransferenciaPossivel(Long fromId, BigDecimal amount) {
        if (fromId == null || amount == null) {
            return false;
        }
        
        Optional<Beneficio> fromOpt = beneficioRepository.findById(fromId);
        if (fromOpt.isEmpty()) {
            return false;
        }
        
        Beneficio from = fromOpt.get();
        return from.getAtivo() && 
               from.getValor().compareTo(amount) >= 0 &&
               amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    // Resto dos métodos do service...
    public Beneficio criarBeneficio(Beneficio beneficio) {
        if (beneficio == null) {
            throw new IllegalArgumentException("Benefício não pode ser nulo");
        }
        return beneficioRepository.save(beneficio);
    }
    
    public Optional<Beneficio> buscarPorId(Long id) {
        return beneficioRepository.findById(id);
    }
    
    public java.util.List<Beneficio> listarTodos() {
        return beneficioRepository.findAll();
    }
    
    public java.util.List<Beneficio> listarAtivos() {
        return beneficioRepository.findByAtivoTrue();
    }
}