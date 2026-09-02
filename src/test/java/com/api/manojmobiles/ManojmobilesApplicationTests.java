package com.api.manojmobiles;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
    "PINELABS_CLIENT_ID=mock",
    "PINELABS_CLIENT_SECRET=mock",
    "PINELABS_BASE_URL=mock",
    "CLOUDINARY_URL=cloudinary://mock:mock@mock"
})
class ManojmobilesApplicationTests {

	@Test
	void contextLoads() {
	}

}
