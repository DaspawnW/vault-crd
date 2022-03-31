package de.koudingspawn.vault.config;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

// https://www.cyberkendra.com/2022/03/springshell-rce-0-day-vulnerability.html
@ControllerAdvice
@Order(10000)
public class GlobalControllerAdvice {

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        String[] abd = new String[]{"class.*", "Class.*", "*.class.*", "*.Class.*"};
        dataBinder.setDisallowedFields(abd);
    }

}