package com.demo.nimn.config.security;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "prod"})
class ProdApiAccessControlTest extends AbstractApiAccessControlTest {
}
