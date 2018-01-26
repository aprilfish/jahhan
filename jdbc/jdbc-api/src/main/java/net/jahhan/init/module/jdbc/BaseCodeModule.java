package net.jahhan.init.module.jdbc;

import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import lombok.extern.slf4j.Slf4j;
import net.jahhan.common.extension.constant.BaseConfiguration;
import net.jahhan.common.extension.utils.ClassScaner;
import net.jahhan.init.InitAnnocation;

@InitAnnocation(isLazy = false, initSequence = 1300)
@Slf4j
public class BaseCodeModule extends AbstractModule {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void configure() {
		log.info("scan dao now!");
		Set<Class> classes = ClassScaner.findClassInPath(".+Dao",
				BaseConfiguration.SERVICE_PATH.replace(".", "/") + "/dao/");
		for (Class daoClass : classes) {
			String cacheName = BaseConfiguration.SERVICE_PATH + ".dao.impl." + daoClass.getSimpleName()
					+ "Impl";
			try {
				Class<?> implClass = Class.forName(cacheName);
				bind(daoClass).to(implClass);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Set<Class> frameworkClasses = ClassScaner.findClassInPath(".+Dao",
				BaseConfiguration.FRAMEWORK_PATH.replace(".", "/") + "/dao/");
		for (Class daoClass : frameworkClasses) {
			String cacheName = BaseConfiguration.FRAMEWORK_PATH + ".dao.impl." + daoClass.getSimpleName()
					+ "Impl";
			try {
				Class<?> implClass = Class.forName(cacheName);
				bind(daoClass).to(implClass);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		log.info("scan rep now!");
		Set<Class> listenClasses = ClassScaner.findClassInPath(".+Rep",
				BaseConfiguration.SERVICE_PATH.replace(".", "/") + "/dao/listen/");
		for (Class listenClass : listenClasses) {
			try {
				bind(listenClass).in(Scopes.SINGLETON);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Set<Class> frameworkListenClasses = ClassScaner.findClassInPath(".+Dao",
				BaseConfiguration.FRAMEWORK_PATH.replace(".", "/") + "/dao/listen/");
		for (Class listenClass : frameworkListenClasses) {
			try {
				bind(listenClass).in(Scopes.SINGLETON);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
