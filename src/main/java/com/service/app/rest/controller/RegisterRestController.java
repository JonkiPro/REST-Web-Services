package com.service.app.rest.controller;

import com.service.app.converter.UnidirectionalConverter;
import com.service.app.entity.User;
import com.service.app.rest.request.RegisterDTO;
import com.service.app.security.SecurityRole;
import com.service.app.service.MailService;
import com.service.app.service.UserService;
import com.service.app.utils.RandomUtils;
import com.service.app.validator.component.RegisterPasswordsValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@PreAuthorize("hasRole('ROLE_ANONYMOUS')")
@RequestMapping(value = "/register")
@Api(value = "Register API", description = "Provides a list of methods for registration")
public class RegisterRestController {

    @Autowired
    private UserService userService;
    @Autowired
    private MailService mailService;
    @Autowired
    private RegisterPasswordsValidator registerPasswordsValidator;
    @Autowired
    private UnidirectionalConverter<RegisterDTO, User> converterRegisterDTOToUser;

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.addValidators(registerPasswordsValidator);
    }

    @ApiOperation(value = "Register the user", code = 201)
    @PostMapping
    public
    HttpEntity<Boolean> register(
            @RequestBody @Valid RegisterDTO registerDTO
    ) {
        User user = converterRegisterDTOToUser.convert(registerDTO);
        user.setActivationToken(RandomUtils.randomToken());
        user.setAuthorities(SecurityRole.ROLE_USER.toString());

        mailService.sendMailWithActivationToken(user.getEmail(), user.getActivationToken());

        userService.saveUser(user);

        return new ResponseEntity<>(true, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Activate the user with token.")
    @ApiResponses(value = { @ApiResponse(code = 404, message = "Token not found") })
    @PutMapping(value = "/token/{token}")
    public
    HttpEntity<Boolean> confirmAccount(
            @PathVariable String token
    ) {
        Optional<User> userOptional = userService.findByActivationToken(token);

        if(!userOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();

        user.setActivationToken(null);
        user.setEnabled(true);

        userService.saveUser(user);

        return ResponseEntity.ok(true);
    }
}