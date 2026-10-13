
package com.unifiedinbox.unified_inbox.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(annotations = Controller.class)
public class OrganizationAccessExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public String handleAccessError(
            ResponseStatusException exception,
            RedirectAttributes redirectAttributes
    ) {
        if (exception.getStatusCode().value()
                != HttpStatus.NOT_FOUND.value()) {
            throw exception;
        }

        redirectAttributes.addFlashAttribute(
                "error",
                "You no longer have access to this organization."
        );

        return "redirect:/organizations";
    }
}
