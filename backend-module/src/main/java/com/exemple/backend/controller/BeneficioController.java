package com.exemple.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.exemple.backend.entity.Beneficio;
import com.exemple.backend.service.BeneficioService;

import java.math.BigDecimal;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/beneficios")
@Tag(name = "Benefícios", description = "API para gerenciamento de benefícios financeiros")
public class BeneficioController {
    
    @Autowired
    private BeneficioService beneficioService;

    @Operation(summary = "Listar todos os benefícios", description = "Retorna uma lista com todos os benefícios cadastrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de benefícios retornada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping
    public ResponseEntity<List<Beneficio>> listarTodos() {
        return ResponseEntity.ok(beneficioService.listarTodos());
    }
    
    @Operation(summary = "Transferir valor entre benefícios", description = "Realiza transferência usando OPTIMISTIC LOCKING (padrão)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
        @ApiResponse(responseCode = "409", description = "Conflito de concorrência"),
        @ApiResponse(responseCode = "422", description = "Erro de negócio (saldo insuficiente, benefício inativo)"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping("/transferir")
    public ResponseEntity<Map<String, Object>> transferir(
            @Parameter(description = "ID do benefício de origem", example = "1", required = true)
            @RequestParam Long fromId,
            
            @Parameter(description = "ID do benefício de destino", example = "2", required = true)
            @RequestParam Long toId,
            
            @Parameter(description = "Valor da transferência", example = "100.00", required = true)
            @RequestParam BigDecimal amount) {
        
        return executarTransferencia(() -> beneficioService.transfer(fromId, toId, amount), 
                                   fromId, toId, amount, "optimistic");
    }

    @Operation(summary = "Transferir com PESSIMISTIC LOCKING", description = "Realiza transferência usando PESSIMISTIC LOCKING para alta contenção")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
        @ApiResponse(responseCode = "422", description = "Erro de negócio"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping("/transferir/pessimistic")
    public ResponseEntity<Map<String, Object>> transferirComPessimisticLock(
            @Parameter(description = "ID do benefício de origem", example = "1", required = true)
            @RequestParam Long fromId,
            
            @Parameter(description = "ID do benefício de destino", example = "2", required = true)
            @RequestParam Long toId,
            
            @Parameter(description = "Valor da transferência", example = "100.00", required = true)
            @RequestParam BigDecimal amount) {
        
        return executarTransferencia(() -> beneficioService.transferWithPessimisticLock(fromId, toId, amount), 
                                   fromId, toId, amount, "pessimistic");
    }

    @Operation(summary = "Transferir com MIXED LOCKING", description = "Realiza transferência usando MIXED LOCKING (pessimistic na origem, optimistic no destino)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
        @ApiResponse(responseCode = "409", description = "Conflito de concorrência no destino"),
        @ApiResponse(responseCode = "422", description = "Erro de negócio"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping("/transferir/mixed")
    public ResponseEntity<Map<String, Object>> transferirComMixedLock(
            @Parameter(description = "ID do benefício de origem", example = "1", required = true)
            @RequestParam Long fromId,
            
            @Parameter(description = "ID do benefício de destino", example = "2", required = true)
            @RequestParam Long toId,
            
            @Parameter(description = "Valor da transferência", example = "100.00", required = true)
            @RequestParam BigDecimal amount) {
        
        return executarTransferencia(() -> beneficioService.transferWithMixedLock(fromId, toId, amount), 
                                   fromId, toId, amount, "mixed");
    }

    // Método auxiliar para executar transferências
    private ResponseEntity<Map<String, Object>> executarTransferencia(
            Runnable transferenciaMethod, Long fromId, Long toId, BigDecimal amount, String lockType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            transferenciaMethod.run();

            response.put("success", true);
            response.put("message", "Transferência realizada com sucesso usando " + lockType + " locking");
            response.put("fromId", fromId);
            response.put("toId", toId);
            response.put("amount", amount);
            response.put("lockType", lockType);
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Erro de validação: " + e.getMessage());
            response.put("lockType", lockType);
            response.put("timestamp", new Date());
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", "Erro de negócio: " + e.getMessage());
            response.put("lockType", lockType);
            response.put("timestamp", new Date());
            return ResponseEntity.unprocessableEntity().body(response);

        } catch (RuntimeException e) {
            // Captura exceções de concorrência
            if (e.getMessage().contains("concorrência") || e.getMessage().contains("conflito")) {
                response.put("success", false);
                response.put("message", "Conflito de concorrência: " + e.getMessage());
                response.put("lockType", lockType);
                response.put("timestamp", new Date());
                return ResponseEntity.status(409).body(response); // 409 Conflict
            }
            
            response.put("success", false);
            response.put("message", "Erro interno: " + e.getMessage());
            response.put("lockType", lockType);
            response.put("timestamp", new Date());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "Obter versão do benefício", description = "Retorna a versão atual do benefício para controle de concorrência")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Versão obtida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Benefício não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/{id}/versao")
    public ResponseEntity<Map<String, Object>> obterVersao(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long versao = beneficioService.obterVersaoAtual(id);
            
            response.put("success", true);
            response.put("beneficioId", id);
            response.put("versao", versao);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro: " + e.getMessage());
            response.put("beneficioId", id);
            response.put("timestamp", new Date());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Verificar conflito de versão", description = "Verifica se há conflito entre a versão informada e a versão atual")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificação realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/verificar-conflito")
    public ResponseEntity<Map<String, Object>> verificarConflito(
            @RequestParam Long beneficioId,
            @RequestParam Long versao) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean temConflito = beneficioService.verificarConflitoVersao(beneficioId, versao);
            
            response.put("success", true);
            response.put("beneficioId", beneficioId);
            response.put("versaoInformada", versao);
            response.put("temConflito", temConflito);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro: " + e.getMessage());
            response.put("beneficioId", beneficioId);
            response.put("versaoInformada", versao);
            response.put("timestamp", new Date());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Consultar saldo", description = "Retorna o saldo atual de um benefício")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Saldo obtido com sucesso"),
        @ApiResponse(responseCode = "400", description = "Benefício não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/{id}/saldo")
    public ResponseEntity<Map<String, Object>> consultarSaldo(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            BigDecimal saldo = beneficioService.consultarSaldo(id);
            
            response.put("success", true);
            response.put("beneficioId", id);
            response.put("saldo", saldo);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Erro: " + e.getMessage());
            response.put("beneficioId", id);
            response.put("timestamp", new Date());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro interno: " + e.getMessage());
            response.put("beneficioId", id);
            response.put("timestamp", new Date());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "Verificar transferência possível", description = "Verifica se uma transferência é possível sem executá-la")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificação realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/verificar-transferencia")
    public ResponseEntity<Map<String, Object>> verificarTransferencia(
            @RequestParam Long fromId,
            @RequestParam BigDecimal amount) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isPossivel = beneficioService.isTransferenciaPossivel(fromId, amount);
            
            response.put("success", true);
            response.put("fromId", fromId);
            response.put("amount", amount);
            response.put("transferenciaPossivel", isPossivel);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro na verificação: " + e.getMessage());
            response.put("fromId", fromId);
            response.put("amount", amount);
            response.put("timestamp", new Date());
            return ResponseEntity.badRequest().body(response);
        }
    }
}