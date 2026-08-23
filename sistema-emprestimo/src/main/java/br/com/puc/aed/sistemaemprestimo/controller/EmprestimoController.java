package br.com.puc.aed.sistemaemprestimo.controller;

import br.com.puc.aed.sistemaemprestimo.domain.SolicitarEmprestimoVO;
import br.com.puc.aed.sistemaemprestimo.service.EmprestimoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    @PostMapping("/solicitar")
    public ResponseEntity<Void> solicitar(@RequestBody SolicitarEmprestimoVO request) {
        emprestimoService.solictar(request);
        return ResponseEntity.accepted().build();
    }
}
