/*
    Copyright 2015 Australian National Botanic Gardens

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

package services

import au.org.biodiversity.nsl.Author
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Notification


class CheckNamesJob {

    def nameService

    def concurrent = false
    def sessionRequired = true

    static triggers = {
        simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute() {
        Name.withTransaction {
            List<Notification> notifications = Notification.list()
            notifications.each { Notification note ->
                switch (note.message) {
                    case 'name updated':
                        log.debug "Name $note.objectId updated"
                        Name name = Name.get(note.objectId)
                        if (name) {
                            nameService.nameUpdated(name, note)
                        } else {
                            log.debug "Name $note.objectId  doesn't exist "
                        }
                        break
                    case 'name created':
                        log.debug "Name $note.objectId created"
                        Name name = Name.get(note.objectId)
                        if (name) {
                            nameService.nameCreated(name, note)
                        } else {
                            log.debug "Name $note.objectId doesn't exist"
                        }
                        break
                    case 'name deleted':
                        log.info "Name $note.objectId was deleted."
                        break
                    case 'author updated':
                        log.debug "Author $note.objectId updated"
                        Author author = Author.get(note.objectId)
                        if (author) {
                            nameService.authorUpdated(author, note)
                        } else {
                            log.debug "Author $note.objectId  doesn't exist "
                        }
                        break
                    case 'author created':
                    case 'author deleted':
                            //NSL-1032 ignore for now, deleted authors can't have names
                        break
                    default:
                        //probably caused by previous error. This note will be deleted
                        log.error "unhandled notification $note.message:$note.objectId"
                }
                note.delete()
            }
        }
    }
}
