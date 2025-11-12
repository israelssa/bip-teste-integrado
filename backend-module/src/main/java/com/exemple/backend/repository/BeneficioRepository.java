package com.exemple.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.exemple.backend.entity.Beneficio;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    // Buscar benefícios ativos
    List<Beneficio> findByAtivoTrue();
    
    // Buscar benefício por nome
    Optional<Beneficio> findByNome(String nome);
    
    // Buscar benefícios com valor maior que
    List<Beneficio> findByValorGreaterThan(BigDecimal valor);
    
    // Buscar benefícios por nome contendo string (case insensitive)
    List<Beneficio> findByNomeContainingIgnoreCase(String nome);
    
    // Buscar benefícios ativos por valor entre
    @Query("SELECT b FROM Beneficio b WHERE b.ativo = true AND b.valor BETWEEN :minValor AND :maxValor")
    List<Beneficio> findBeneficiosAtivosComValorEntre(@Param("minValor") BigDecimal minValor, 
                                                     @Param("maxValor") BigDecimal maxValor);
    
    // Contar benefícios ativos
    long countByAtivoTrue();
    
    // MÉTODOS COM LOCKING
    
    /**
     * Busca benefício com PESSIMISTIC WRITE lock
     * Bloqueia a linha no banco até o fim da transação
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Beneficio b WHERE b.id = :id")
    Optional<Beneficio> findByIdWithPessimisticLock(@Param("id") Long id);
    
    /**
     * Busca benefício com PESSIMISTIC READ lock
     * Permite leitura concorrente mas bloqueia escritas
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT b FROM Beneficio b WHERE b.id = :id")
    Optional<Beneficio> findByIdWithPessimisticRead(@Param("id") Long id);
    
    /**
     * Busca benefício com OPTIMISTIC lock
     * Usa a anotação @Version para controle de concorrência
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT b FROM Beneficio b WHERE b.id = :id")
    Optional<Beneficio> findByIdWithOptimisticLock(@Param("id") Long id);
    
    /**
     * Busca múltiplos benefícios com lock pessimista
     * Útil para transferências que precisam lock em vários registros
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Beneficio b WHERE b.id IN :ids")
    List<Beneficio> findAllByIdWithPessimisticLock(@Param("ids") List<Long> ids);
}