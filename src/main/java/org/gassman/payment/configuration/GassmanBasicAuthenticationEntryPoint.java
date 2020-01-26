package org.gassman.payment.configuration;

import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

public class GassmanBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {
    public static String REALM="GASSMAN_REST_API";

    @Override
    public void afterPropertiesSet() throws Exception {
        setRealmName(REALM);
        super.afterPropertiesSet();
    }
}
