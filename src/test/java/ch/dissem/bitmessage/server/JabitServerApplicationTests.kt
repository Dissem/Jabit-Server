package ch.dissem.bitmessage.server

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [JabitServerApplication::class])
class JabitServerApplicationTests {

    @Test
    fun contextLoads() = Unit

}
