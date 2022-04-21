package com.common;

import io.grpc.Context;
import io.grpc.Metadata;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

public class Constant {
    public static final Metadata.Key<String> HEADER_ROLE = Metadata.Key.of("role_name",Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<SecurityProperties.User> CONTEXT_ROLE = Context.key("role_name");
}
