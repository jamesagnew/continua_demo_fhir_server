package ca.uhn.fhir.jpa.demo;

import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu1;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu1;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

public class JpaServerDemo extends RestfulServer {

	private static final long serialVersionUID = 1L;

	private WebApplicationContext myAppCtx;

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		/* 
		 * We want to support FHIR DSTU2 format. This means that the server
		 * will use the DSTU2 bundle format and other DSTU2 encoding changes.
		 *
		 * If you want to use DSTU1 instead, change the following line, and change the 2 occurrences of dstu2 in web.xml to dstu1
		 */
		FhirVersionEnum fhirVersion = FhirVersionEnum.DSTU2;
		setFhirContext(new FhirContext(fhirVersion));

		// Get the spring context from the web container (it's declared in web.xml)
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

		/* 
		 * The hapi-fhir-server-resourceproviders-dev.xml file is a spring configuration
		 * file which is automatically generated as a part of hapi-fhir-jpaserver-base and
		 * contains bean definitions for a resource provider for each resource type
		 */
		String resourceProviderBeanName = "myResourceProvidersDstu" + (fhirVersion == FhirVersionEnum.DSTU1 ? "1" : "2");
		List<IResourceProvider> beans = myAppCtx.getBean(resourceProviderBeanName, List.class);
		setResourceProviders(beans);
		
		/* 
		 * The system provider implements non-resource-type methods, such as
		 * transaction, and global history.
		 */
		Object systemProvider = myAppCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class);
		setPlainProviders(systemProvider);

		IFhirSystemDao<Bundle> systemDao = myAppCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
		JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao, myAppCtx.getBean(DaoConfig.class));
		confProvider.setImplementationDescription("Example Server");
		setServerConformanceProvider(confProvider);

		/*
		 * This tells the server to use "browser friendly" MIME types if it 
		 * detects that the request is coming from a browser, in the hopes that the 
		 * browser won't just treat the content as a binary payload and try 
		 * to download it (which is what generally happens if you load a 
		 * FHIR URL in a browser). 
		 * 
		 * This means that the server isn't technically complying with the 
		 * FHIR specification for direct browser requests, but this mode
		 * is very helpful for testing and troubleshooting since it means 
		 * you can look at FHIR URLs directly in a browser.  
		 */
		setUseBrowserFriendlyContentTypes(true);

		/*
		 * Default to XML and pretty printing
		 */
		setDefaultPrettyPrint(true);
		setDefaultResponseEncoding(EncodingEnum.JSON);

		setServerAddressStrategy(new HardcodedServerAddressStrategy("http://continua.cloudapp.net/baseDstu2"));
		
		/*
		 * This is a simple paging strategy that keeps the last 10 searches in memory
		 */
		FifoMemoryPagingProvider pagingProvider = new FifoMemoryPagingProvider(100);
		pagingProvider.setMaximumPageSize(5000);
		setPagingProvider(pagingProvider);

		/*
		 * Load interceptors for the server from Spring (these are defined in AppCtxConfig.java)
		 */
		Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
		for (IServerInterceptor interceptor : interceptorBeans) {
			this.registerInterceptor(interceptor);
		}

	}

}
