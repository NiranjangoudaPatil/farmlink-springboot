package com.farmlink.mail;

public record EmailEvent(String to, String subject, String body) {}
