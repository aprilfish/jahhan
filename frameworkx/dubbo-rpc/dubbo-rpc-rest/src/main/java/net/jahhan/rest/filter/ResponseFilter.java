package net.jahhan.rest.filter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Date;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.util.HttpResponseCodes;

import net.jahhan.common.extension.constant.BaseConfiguration;
import net.jahhan.common.extension.constant.JahhanErrorCode;
import net.jahhan.common.extension.utils.LocalIpUtils;
import net.jahhan.exception.ExceptionMessage;

@Provider
public class ResponseFilter implements ContainerResponseFilter {
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {

		if (HttpResponseCodes.SC_NOT_FOUND == responseContext.getStatus()) {
			String escapedMsg = "接口不存在";
			ExceptionMessage exceptionMessage = new ExceptionMessage();
			exceptionMessage.setHttpStatus(404);
			exceptionMessage.setCode(JahhanErrorCode.UNKNOW_SERVICE_EXCEPTION);
			exceptionMessage.setMessage(escapedMsg);
			exceptionMessage.setService(BaseConfiguration.SERVICE);
			exceptionMessage.setHost(LocalIpUtils.getFirstIp());
			exceptionMessage.setThreadId(Thread.currentThread().getId());
			exceptionMessage.setThreadName(Thread.currentThread().getName());
			exceptionMessage.setTime(new Date());
			responseContext.setEntity(exceptionMessage, new Annotation[] {}, MediaType.valueOf("application/json"));
		} else if (HttpResponseCodes.SC_BAD_REQUEST == responseContext.getStatus()) {
			Object entity = responseContext.getEntity();
			if (entity != null && entity instanceof String
					&& entity.equals("java.io.EOFException: No content to map to Object due to end of input")) {
				ExceptionMessage exceptionMessage = new ExceptionMessage();
				exceptionMessage.setHttpStatus(404);
				exceptionMessage.setCode(JahhanErrorCode.VALIATION_EXCEPTION);
				exceptionMessage.setMessage("错误的请求内容");
				exceptionMessage.setService(BaseConfiguration.SERVICE);
				exceptionMessage.setHost(LocalIpUtils.getFirstIp());
				exceptionMessage.setThreadId(Thread.currentThread().getId());
				exceptionMessage.setThreadName(Thread.currentThread().getName());
				exceptionMessage.setTime(new Date());
				responseContext.setEntity(exceptionMessage, new Annotation[] {}, MediaType.valueOf("application/json"));
			}
		}
	}
}