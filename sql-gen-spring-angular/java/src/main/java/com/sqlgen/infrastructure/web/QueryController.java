package com.sqlgen.infrastructure.web;

import com.sqlgen.application.dto.QuestionRequest;
import com.sqlgen.domain.port.in.ProcessQuestionUseCase;
import com.sqlgen.infrastructure.web.dto.QueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST principal: recibe preguntas del usuario, las procesa y devuelve
 * el SQL + las filas resultantes (o un error).
 */
@RestController
@RequestMapping("/api/v1/queries")
@Tag(name = "queries", description = "Procesar preguntas en lenguaje natural")
public class QueryController {

    private final ProcessQuestionUseCase processQuestionUseCase;

    public QueryController(ProcessQuestionUseCase processQuestionUseCase) {
        this.processQuestionUseCase = processQuestionUseCase;
    }

    @PostMapping
    @Operation(summary = "Procesa una pregunta y devuelve SQL + resultados")
    public ResponseEntity<QueryResponse> ask(@Valid @RequestBody QuestionRequest request) {
        var question = request.toDomain();
        var result = processQuestionUseCase.process(question);
        var response = QueryResponse.from(
            result.sql(),
            result.rows(),
            result.columns(),
            result.error()
        );
        return ResponseEntity.ok(response);
    }
}
