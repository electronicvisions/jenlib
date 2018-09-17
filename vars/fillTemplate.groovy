import com.cloudbees.groovy.cps.NonCPS
import groovy.text.StreamingTemplateEngine

/**
 * Fill a Template using {@link StreamingTemplateEngine}.
 *
 * @param template Template to be filled
 * @param parameters Map of template parameters
 * @return Filled template
 */
@NonCPS
static call(String template, Map<String, String> parameters) {
	StreamingTemplateEngine templateEngine = new StreamingTemplateEngine()
	return templateEngine.createTemplate(template).make(parameters).toString()
}
