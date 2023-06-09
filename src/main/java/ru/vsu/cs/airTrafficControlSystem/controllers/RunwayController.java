package ru.vsu.cs.airTrafficControlSystem.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.airTrafficControlSystem.dto.RunwayDTO;
import ru.vsu.cs.airTrafficControlSystem.exceptions.RunwayNotCreatedException;
import ru.vsu.cs.airTrafficControlSystem.exceptions.RunwayNotFoundException;
import ru.vsu.cs.airTrafficControlSystem.models.Airport;
import ru.vsu.cs.airTrafficControlSystem.models.Runway;
import ru.vsu.cs.airTrafficControlSystem.services.AirportService;
import ru.vsu.cs.airTrafficControlSystem.services.RunwayService;
import ru.vsu.cs.airTrafficControlSystem.util.ErrorResponse;
import java.util.List;
import java.util.stream.Collectors;
import static ru.vsu.cs.airTrafficControlSystem.util.ErrorsUtil.returnErrorsToClient;

@RestController
@RequestMapping("/api/runways")
@Tag(name = "Runway Controller", description = "Interaction with runways")
public class RunwayController {
    private final RunwayService runwayService;
    private final ModelMapper modelMapper;
    private final AirportService airportService;

    @Autowired
    public RunwayController(RunwayService runwayService, ModelMapper modelMapper, AirportService airportService, AirportController airportController) {
        this.runwayService = runwayService;
        this.modelMapper = modelMapper;
        this.airportService = airportService;
    }

    @GetMapping
    @Operation(summary = "Get all runways")
    public List<RunwayDTO> getRunways() {
        return runwayService.getRunways().stream().map(this::convertToRunwayDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get runway by id")
    public RunwayDTO getRunway(@PathVariable("id") int id) {
        return convertToRunwayDTO(runwayService.getRunwayById(id));
    }

    @PostMapping("/create")
    @Operation(summary = "Create runway")
    public ResponseEntity<HttpStatus> create(@RequestBody @Valid RunwayDTO runwayDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMsg = returnErrorsToClient(bindingResult);
            throw new RunwayNotCreatedException(errorMsg);
        }
        Airport airport = airportService.getAirportByNameAndLocation(runwayDTO.getAirportDTO().getName(),
                                                                    runwayDTO.getAirportDTO().getLocation());
        Runway runway = convertToRunway(runwayDTO);
        runway.setAirport(airport);
        runwayService.addRunway(runway);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete runway")
    public ResponseEntity<HttpStatus> delete(@PathVariable("id") int id) {
        runwayService.deleteRunway(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/update/{id}")
    @Operation(summary = "Update runway")
    public ResponseEntity<HttpStatus> update(@PathVariable("id") int id, @RequestBody RunwayDTO runwayDTO) {
        runwayService.updateRunway(id, convertToRunway(runwayDTO));
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(RunwayNotFoundException e) {
        ErrorResponse response = new ErrorResponse(
                "Runway with such id wasn't found!", System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> handleException(RunwayNotCreatedException e) {
        ErrorResponse response = new ErrorResponse(
                e.getMessage(), System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private Runway convertToRunway(RunwayDTO runwayDTO) {
        return modelMapper.map(runwayDTO, Runway.class);
    }

    private RunwayDTO convertToRunwayDTO(Runway runway) {
        return modelMapper.map(runway, RunwayDTO.class);
    }

}
