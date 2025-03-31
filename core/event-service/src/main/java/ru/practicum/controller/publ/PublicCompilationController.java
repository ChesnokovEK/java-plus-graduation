package ru.practicum.controller.publ;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.service.CompilationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getAll(@RequestParam(value = "pinned", defaultValue = "false") Boolean pinned,
                                       @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero int from,
                                       @RequestParam(value = "size", defaultValue = "10") @Positive int size) {
        return compilationService.getAll(pinned, from, size);
    }

    @GetMapping("/{id}")
    public CompilationDto getById(@PathVariable("id") long id) {
        return compilationService.getById(id);
    }
}
