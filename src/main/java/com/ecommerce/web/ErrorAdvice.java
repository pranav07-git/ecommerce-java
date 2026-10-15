package com.ecommerce.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/** Turns business-rule violations into a flash error and sends the user back where they came from. */
@ControllerAdvice
public class ErrorAdvice {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handle(RuntimeException e, HttpServletRequest req, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        String target = "/";
        String referer = req.getHeader("Referer");
        if (referer != null) {
            try {
                var uri = UriComponentsBuilder.fromUriString(referer).build();
                // Keep only path + query so the redirect can never leave this site.
                if (uri.getPath() != null && uri.getPath().startsWith("/")) {
                    target = uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
                }
            } catch (IllegalArgumentException ignored) {
                // fall back to "/"
            }
        }
        return "redirect:" + target;
    }
}
