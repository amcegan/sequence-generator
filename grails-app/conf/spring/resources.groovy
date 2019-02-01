import com.centaur.plugins.sequence.DefaultSequenceGenerator
import org.springframework.jmx.export.MBeanExporter
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
import org.springframework.jmx.support.MBeanServerFactoryBean

// Place your Spring DSL code here
beans = {
    sequenceGenerator(DefaultSequenceGenerator) { bean ->
        bean.autowire = 'byName'
    }

    //create/find the mbean server
    mbeanServer(MBeanServerFactoryBean) {
        locateExistingServerIfPossible = true
    }
    //use annotations for attributes/operations
    jmxAttributeSource(AnnotationJmxAttributeSource)

    assembler(MetadataMBeanInfoAssembler) {
        attributeSource = jmxAttributeSource
    }
    //create an exporter that uses annotations
    annotationExporter(MBeanExporter) {
        server = mbeanServer
        beans = [:]
    }
}