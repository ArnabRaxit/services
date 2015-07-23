package au.org.biodiversity.nsl

import grails.converters.XML
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils
import org.springframework.http.HttpStatus

class AuthController {
    def shiroSecurityManager
    def grailsApplication

    def index = { redirect(action: "login", params: params) }

    def login = {
        return [username: params.username, rememberMe: (params.rememberMe != null), targetUri: params.targetUri]
    }

    def signIn = {
        def authToken = new UsernamePasswordToken(params.username, params.password as String)

        // Support for "remember me"
        if (params.rememberMe) {
            authToken.rememberMe = true
        }

        // If a controller redirected to this page, redirect back
        // to it. Otherwise redirect to the root URI.
        String rootURI = grailsApplication.config.grails.serverURL
        def targetUri = params.targetUri ?: rootURI

        // Handle requests saved by Shiro filters.
        SavedRequest savedRequest = WebUtils.getSavedRequest(request)
        if (savedRequest) {
            targetUri = savedRequest.requestURI - request.contextPath
            if (savedRequest.queryString) targetUri = targetUri + '?' + savedRequest.queryString
        }

        try {
            // Perform the actual login. An AuthenticationException
            // will be thrown if the username is unrecognised or the
            // password is incorrect.
            SecurityUtils.subject.login(authToken)

            log.info "Redirecting to '${targetUri}'."
            redirect(uri: targetUri)
        }
        catch (AuthenticationException ex) {
            // Authentication failed, so display the appropriate message
            // on the login page.
            log.info "Authentication failure for user '${params.username}'."
            flash.message = message(code: "login.failed")

            // Keep the username and "remember me" setting so that the
            // user doesn't have to enter them again.
            def m = [username: params.username]
            if (params.rememberMe) {
                m["rememberMe"] = true
            }

            // Remember the target URI too.
            if (params.targetUri) {
                m["targetUri"] = params.targetUri
            }

            // Now redirect back to the login page.
            redirect(action: "login", params: m)
        }
    }

    def signOut = {
        // Log the user out of the application.
        SecurityUtils.subject?.logout()
        webRequest.getCurrentRequest().session = null

        // For now, redirect back to the home page.
        redirect(controller: 'search')
    }

    def unauthorized = {
        withFormat {
            html {
                render(status: 401, text: "You do not have permission to do that.")
            }
            json {
                def error = mapError()
                render(contentType: "application/json", status: 401){
                    error
                }
            }
            xml {
                def error = mapError()
                response.status = 401
                render error as XML
            }
        }
    }

    private Map mapError() {
        Map errorMap = [:]
        errorMap.status = prettyPrintStatus(401)
        errorMap.uri = (org.codehaus.groovy.grails.web.util.WebUtils.getForwardURI(request) ?: request.getAttribute('javax.servlet.error.request_uri'))
        errorMap.reason = "You do not have permission."
        return errorMap
    }

    private static String prettyPrintStatus(int statusCode) {
        String httpStatusReason = HttpStatus.valueOf(statusCode).getReasonPhrase()
        "$statusCode: ${httpStatusReason}"
    }

}
