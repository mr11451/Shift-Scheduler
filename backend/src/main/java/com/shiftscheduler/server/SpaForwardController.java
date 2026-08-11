package com.shiftscheduler.server;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SpaForwardController {

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
