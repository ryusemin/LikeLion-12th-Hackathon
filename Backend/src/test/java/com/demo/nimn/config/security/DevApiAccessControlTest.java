package com.demo.nimn.config.security;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "dev"})
class DevApiAccessControlTest extends AbstractApiAccessControlTest {
}
