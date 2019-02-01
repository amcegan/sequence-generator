package com.centaur.plugins.sequence

import grails.plugins.*
import org.springframework.context.ApplicationContext
import org.springframework.jmx.export.MBeanExporter
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
import org.springframework.jmx.support.MBeanServerFactoryBean

import javax.management.MBeanServer
import javax.management.ObjectName
import java.lang.management.ManagementFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SequenceGeneratorGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.8 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Sequence Generator" // Headline display name of the plugin
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def description = '''A Grails service that generate sequence numbers from different sequences, formats, etc.
You can control the starting number, the format and you can have different sequences based on application logic.
The method getNextSequenceNumber() is injected into all domain classes annotated with @SequenceEntity.
It returns the next number for the sequence defined for the domain class.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/sequence-generator"

    private Logger LOG = LoggerFactory.getLogger('grails.plugins.sequence.SequenceGeneratorGrailsPlugin')

    private final String JMX_OBJECT_NAME = ':name=SequenceGeneratorService,type=services'


    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->

        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        def config = grailsApplication.config
        for (c in grailsApplication.domainClasses) {
            if (c.clazz.getAnnotation(SequenceEntity)) {
                addDomainMethods(applicationContext, config, c.metaClass)
            }
        }
        registerJMX(applicationContext, grailsApplication.metadata.getApplicationName() + JMX_OBJECT_NAME)
    }

    private void addDomainMethods(ctx, config, MetaClass mc) {
        def service = ctx.getBean('sequenceGeneratorService')
        mc.getNextSequenceNumber = { group = null ->
            def name = delegate.class.simpleName
            def tenant = delegate.hasProperty('tenantId') ? delegate.tenantId : null
            def nbr
            delegate.class.withNewSession {
                nbr = service.nextNumber(name, group, tenant)
            }
            return nbr
        }
    }

    private void registerJMX(ApplicationContext ctx, String jmxObjectName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer()
            if (mbs.isRegistered(new ObjectName(jmxObjectName))) {
                LOG.info("MBean $jmxObjectName already registered")
            } else {
                MBeanExporter annotationExporter = ctx.getBean("annotationExporter")
                annotationExporter.beans."$jmxObjectName" = ctx.getBean("sequenceGeneratorService")
                annotationExporter.registerBeans()
            }
        } catch (Exception e) {
            LOG.warn("Failed to register $jmxObjectName", e)
        }
    }

    private void unregisterJMX(ApplicationContext ctx, String jmxObjectName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer()
            if (mbs.isRegistered(new ObjectName(jmxObjectName))) {
                mbs.unregisterMBean(new ObjectName(jmxObjectName))
            } else {
                LOG.info("MBean $jmxObjectName not registered")
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister $jmxObjectName", e)
        }
    }


    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        unregisterJMX(event.ctx, grailsApplication.metadata.getApplicationName() + JMX_OBJECT_NAME)
    }
}
