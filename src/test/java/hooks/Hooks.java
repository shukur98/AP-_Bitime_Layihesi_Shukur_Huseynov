package hooks;

import com.thoughtworks.gauge.AfterScenario;
import com.thoughtworks.gauge.BeforeScenario;
import imp.ApiRequestImp;

/** Gauge-only scenario request/response isolation. */
public class Hooks {
    @BeforeScenario public void before() { ApiRequestImp.clear(); }
    @AfterScenario public void after() { ApiRequestImp.clear(); }
}
