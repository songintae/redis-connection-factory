package com.example.redisconnectionfactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class RedisConnectionFactoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisConnectionFactoryApplication.class, args);
	}

	@Component
	static class TestRunner implements ApplicationRunner, ApplicationContextAware {
		private ApplicationContext applicationContext;

		@Override
		public void run(ApplicationArguments args) throws Exception {
			Map<String, RedisConnectionFactory> beansOfType = applicationContext.getBeansOfType(RedisConnectionFactory.class);
			for (String beanName : beansOfType.keySet()) {
				System.out.println("beanName : " + beanName +", className" + beansOfType.get(beanName).getClass().getName());
			}

		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}
	}

}
