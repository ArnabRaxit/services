/*
    Copyright 2016 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package au.org.biodiversity.nsl

import grails.transaction.Transactional
import groovy.sql.Sql
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.apache.commons.logging.LogFactory

/**
 * This is a helper service for abstracting, accessing and managing configuration of the services.
 *
 * Service configuration is held in these places
 * * database under the shard_config table
 * * ~/home/.nsl/services-config.groovy
 *
 * The services-config file is a standard grails config file that is slurped at startup and can be accessed via the
 * grailsApplication.config object.
 */

@Transactional
class ConfigService {

    GrailsApplication grailsApplication

    private static String configGetOrfail(String key) {
        String value = ShardConfig.findByName(key)?.value
        if (!value) {
            throw new Exception("Config error. Add '$key' to shard_config.")
        }
        return value
    }

    Namespace getNameSpace() {
        String nameSpaceName = configGetOrfail('name space')
        Namespace nameSpace = Namespace.findByName(nameSpaceName)
        if (!nameSpace) {
            log.error "Namespace not correctly set in config. Add 'name space' to shard_config, and make sure Namespace exists."
        }
        return nameSpace
    }

    static String getNameTreeName() {
        return configGetOrfail('name tree label')
    }

    static String getClassificationTreeName() {
        try {
            return configGetOrfail('classification tree key')
        } catch (e) {
            LogFactory.getLog(this).error e.message
        }
        return configGetOrfail('classification tree label')
    }

    static String getShardDescriptionHtml() {
        return configGetOrfail('description html')
    }

    static String getPageTitle() {
        return configGetOrfail('page title')
    }

    static String getBannerText() {
        return configGetOrfail('banner text')
    }

    static String getBannerImage() {
        return configGetOrfail('banner image')
    }

    static String getCardImage() {
        return configGetOrfail('card image')
    }

    static String getProductDescription(String productName) {
        return configGetOrfail("$productName description")
    }

    Sql getSqlForNSLDB() {
        String dbUrl = grailsApplication.config.dataSource_nsl.url
        String username = grailsApplication.config.dataSource_nsl.username
        String password = grailsApplication.config.dataSource_nsl.password
        String driverClassName = grailsApplication.config.dataSource_nsl.driverClassName
        log.debug "Getting sql for $dbUrl, $username, $password, $driverClassName"
        Sql.newInstance(dbUrl, username, password, driverClassName)
    }

    String getWebUserName() {
        grailsApplication.config.shard.webUser
    }

    Map getUpdateScriptParams() {
        [
                webUserName           : getWebUserName(),
                classificationTreeName: classificationTreeName,
                nameTreeName          : nameTreeName,
                nameSpace             : nameSpace
        ]
    }
}
