package com.flippa.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {
    
    @Value("${app.error.show-details:true}")
    private boolean showErrorDetails;
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            model.addAttribute("statusCode", statusCode);
            model.addAttribute("statusText", HttpStatus.valueOf(statusCode).getReasonPhrase());
            
            // Show details only in development mode
            model.addAttribute("showErrorDetails", showErrorDetails);
            if (showErrorDetails) {
                if (exception != null) {
                    model.addAttribute("exception", exception.getClass().getName());
                    model.addAttribute("exceptionMessage", message != null ? message.toString() : "No message available");
                }
                if (requestUri != null) {
                    model.addAttribute("requestUri", requestUri.toString());
                }
            }
            
            // Route to specific error pages
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value() || statusCode == HttpStatus.UNAUTHORIZED.value()) {
                return "error/401";
            } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                return "error/400";
            } else {
                return "error/500";
            }
        }
        
        return "error/500";
    }
}

