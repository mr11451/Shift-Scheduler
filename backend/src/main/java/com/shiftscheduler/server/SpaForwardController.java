package com.shiftscheduler.server;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SpaForwardController {

    /**
     * Forward any non-API, non-file route to index.html so the React router can handle it.
     */
    @GetMapping(value = {
            "/",
            "/{path:[^\\.]*}",
            "/{path1:[^\\.]*}/{path2:[^\\.]*}",
            "/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}"
    })
    public String forwardToIndex(HttpServletRequest request) {
        String requestPath = request.getRequestURI();

        if (requestPath.startsWith("/api")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }

        return "forward:/index.html";
    }
}
