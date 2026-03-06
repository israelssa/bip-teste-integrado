package com.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;

@Stateless
public class BeneficioEjbService {

    @PersistenceContext
    private EntityManager em;

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // Validações iniciais
        if (fromId == null || toId == null || amount == null) {
            throw new IllegalArgumentException("Parâmetros não podem ser nulos");
        }
        
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Não é possível transferir para o mesmo benefício");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transferência deve ser positivo");
        }

        try {
            // Busca com PESSIMISTIC_WRITE para evitar concorrência
            Beneficio from = em.find(Beneficio.class, fromId, LockModeType.PESSIMISTIC_WRITE);
            Beneficio to = em.find(Beneficio.class, toId, LockModeType.PESSIMISTIC_WRITE);
            
            // Valida se os benefícios existem
            if (from == null) {
                throw new IllegalArgumentException("Benefício de origem não encontrado: " + fromId);
            }
            if (to == null) {
                throw new IllegalArgumentException("Benefício de destino não encontrado: " + toId);
            }
            
            // Valida se o benefício de origem está ativo
            if (!from.getAtivo()) {
                throw new IllegalStateException("Benefício de origem não está ativo");
            }
            
            // Valida se o benefício de destino está ativo
            if (!to.getAtivo()) {
                throw new IllegalStateException("Benefício de destino não está ativo");
            }
            
            // Valida saldo suficiente
            if (from.getValor().compareTo(amount) < 0) {
                throw new IllegalStateException(
                    String.format("Saldo insuficiente. Saldo atual: %.2f, Valor solicitado: %.2f", 
                                from.getValor(), amount)
                );
            }
            
            // Valida limites de valor
            if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                throw new IllegalArgumentException("Valor da transferência excede o limite permitido");
            }
            
            // Executa a transferência
            from.setValor(from.getValor().subtract(amount));
            to.setValor(to.getValor().add(amount));
            
            // Atualiza versões para optimistic locking
            from.setVersion(from.getVersion() + 1);
            to.setVersion(to.getVersion() + 1);
            
            // Merge é opcional com PESSIMISTIC_WRITE, mas explícito para clareza
            em.merge(from);
            em.merge(to);
            
            // Flush explícito para garantir persistência
            em.flush();
            
        } catch (PersistenceException e) {
            throw new RuntimeException("Erro na persistência durante transferência", e);
        }
    }
    
    // Método para consulta segura
    public BigDecimal consultarSaldo(Long beneficioId) {
        if (beneficioId == null) {
            throw new IllegalArgumentException("ID do benefício não pode ser nulo");
        }
        
        Beneficio beneficio = em.find(Beneficio.class, beneficioId);
        if (beneficio == null) {
            throw new IllegalArgumentException("Benefício não encontrado: " + beneficioId);
        }
        
        return beneficio.getValor();
    }
    
    // Método para verificar se transferência é possível
    public boolean isTransferenciaPossivel(Long fromId, BigDecimal amount) {
        if (fromId == null || amount == null) {
            return false;
        }
        
        Beneficio from = em.find(Beneficio.class, fromId);
        return from != null && 
               from.getAtivo() && 
               from.getValor().compareTo(amount) >= 0 &&
               amount.compareTo(BigDecimal.ZERO) > 0;
    }
}