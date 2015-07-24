package com.faforever.client.test;

import com.faforever.client.config.TestServiceConfiguration;
import com.faforever.client.config.TestUiConfiguration;
import com.faforever.client.fxml.FxmlLoader;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestUiConfiguration.class, TestServiceConfiguration.class})
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public abstract class AbstractSpringJavaFxTest extends AbstractPlainJavaFxTest {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Autowired
  FxmlLoader fxmlLoader;

  protected void initBean(Object bean, String name) {
    applicationContext.autowireBean(bean);
    applicationContext.initializeBean(bean, name);
  }

  protected <T> T loadController(String fileName) {
    T controller = fxmlLoader.loadAndGetController(fileName);
    initBean(controller, "controller");
    return controller;
  }
}
