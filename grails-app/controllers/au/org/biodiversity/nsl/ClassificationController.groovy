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

package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.ClassificationManagerService
import au.org.biodiversity.nsl.tree.ServiceException
import org.apache.shiro.authz.annotation.RequiresRoles

class ClassificationController {

    ClassificationManagerService classificationManagerService;
    def configService

    private static String VALIDATION_RESULTS_KEY = ClassificationController.class.getName() + '#validationREsults';

    @RequiresRoles('admin')
    def logs() {
        forward controller: "admin", action: "logs"
    }

    @RequiresRoles('admin')
    def index() {
        [
                list             : Arrangement.findAll(sort: 'label') { arrangementType == ArrangementType.P },
                validationResults: session[VALIDATION_RESULTS_KEY]
        ]
    }

    @RequiresRoles('admin')
    def createForm() {
        [list: Arrangement.findAll(sort: 'label') { arrangementType == ArrangementType.P }]
    }

    @RequiresRoles('admin')
    def editForm() {
        Arrangement classification = Arrangement.findByNamespaceAndLabel(configService.nameSpace,
                params['classification'] as String
        );
        [classification: classification, inputLabel: classification.label, inputDescription: classification.description, sharedChk: classification.shared]
    }

    @RequiresRoles('admin')
    def doCreate() {
        // TODO: tell the link service that we have made a classification. It should store
        // [shard]/classification/[label] as a match for the node
        try {
            if (params['Create']) {
                if (!params['inputLabel'] || !params['inputDescription']) {
                    flash.validation = "Label and Description required";
                } else {
                    if (params['copyNameChk']) {
                        Arrangement copyNameIn = Arrangement.findByNamespaceAndLabel(
                                configService.nameSpace,
                                params['inputCopyNameIn'] as String)
                        classificationManagerService.createClassification(configService.nameSpace, label: params.inputLabel, description: params.inputDescription, shared: params.sharedChk ? true : false, copyName: params['inputCopyName'], copyNameIn: copyNameIn);
                    } else {
                        classificationManagerService.createClassification(configService.nameSpace, label: params.inputLabel, description: params.inputDescription, shared: params.sharedChk ? true : false);
                    }

                    flash.success = "Classification \"${params['inputLabel']}\" created."
                    return redirect(action: 'index')
                }
            } else {
                flash.validation = 'No action?'
            }
        }
        catch (ServiceException ex) {
            flash.error = ex.msg.toString();
            // need to roll back?
        }
        catch (Exception ex) {
            log.error(ex)
            flash.error = ex.toString();
        }
        return render(view: 'createForm', model: [
                inputLabel      : params['inputLabel'],
                inputDescription: params['inputDescription'],
                copyNameChk     : params['copyNameChk'],
                inputCopyName   : params['inputCopyName'],
                inputCopyNameIn : params['inputCopyNameIn'],
                sharedChk       : params['sharedChk'],
                list            : Arrangement.findAll(sort: 'label') { arrangementType == ArrangementType.P }
        ])
    }

    @RequiresRoles('admin')
    def doEdit() {
        // TODO: tell the link service that we have updated a classification. It should store
        // [shard]/classification/[label] as a match for the node if the label has changed
        // and remove the match is the classification is deleted

        Arrangement classification = Arrangement.findByNamespaceAndLabel(
                configService.nameSpace,
                params['classification'] as String)

        if (!classification) {
            flash.message = 'no classification specified'
            return redirect(action: 'index')
        }

        try {
            if (params['Cancel']) {
                return render(view: 'editForm', model: [
                        classification  : classification,
                        inputLabel      : params.inputLabel,
                        inputDescription: params.inputDescription
                ])
            } else if (params['Save'] && params.inputLabel != classification.label) {
                if (!params['inputLabel'] || !params['inputDescription']) {
                    flash.validation = 'Label and Description required'
                } else {
                    return render(view: 'editForm', model: [
                            classification     : classification,
                            inputLabel         : params.inputLabel,
                            inputDescription   : params.inputDescription,
                            needsConfirmation  : true,
                            confirmationMessage: "This action will change the persistent, publically-known name of \"${classification.label}\" to ${params.inputLabel}.",
                            confirmationAction : 'Save'
                    ])
                }
            } else if (params['Delete']) {
                return render(view: 'editForm', model: [
                        classification     : classification,
                        inputLabel         : params.inputLabel,
                        inputDescription   : params.inputDescription,
                        needsConfirmation  : true,
                        confirmationMessage: "This action will permanently delete classification \"${classification.label}\" and cannot be undone.",
                        confirmationAction : 'Delete'
                ])
            } else if (params['Ok'] && params['confirmationAction'] == 'Delete') {
                classificationManagerService.deleteClassification(classification);
                flash.success = "Classification \"${classification.label}\" permanently deleted."
                return redirect(action: 'index')
            } else if ((params['Save'] && params.inputLabel == classification.label) || (params['Ok'] && params['confirmationAction'] == 'Save')) {
                if (!params['inputLabel'] || !params['inputDescription']) {
                    flash.validation = 'Label and Description required'
                } else {
                    classificationManagerService.updateClassification(classification, label: params.inputLabel, description: params.inputDescription, shared: params.sharedChk ? true : false);
                    if (params.inputLabel == classification.label) {
                        flash.success = "Classification \"${classification.label}\" updated."
                    } else {
                        flash.success = "Classification \"${classification.label}\" changed to \"${params.inputLabel}\"."
                    }
                    return redirect(action: 'index')
                }
            } else {
                flash.validation = 'No action?'
            }
        }
        catch (ServiceException ex) {
            flash.error = ex.msg.toString();
            // need to roll back?
        }
        catch (Exception ex) {
            log.error(ex)
            flash.error = ex.toString();
            // need to roll back?
        }

        return render(view: 'editForm', model: [
                classification  : classification,
                inputLabel      : params['inputLabel'],
                inputDescription: params['inputDescription']])
    }

    @RequiresRoles('admin')
    def validateClassifications() {
        session[VALIDATION_RESULTS_KEY] = classificationManagerService.validateClassifications()
        session[VALIDATION_RESULTS_KEY].c.each { shardname, classifications ->
            classifications.each { classificationLabel, errors ->
                errors.each { error ->
                    fixLinksFor(error)
                }
            }
        }

        redirect action: "index"
    }

    private void fixLinksFor(result) {
        switch ((ClassificationManagerService.ValidationResult) result.type) {
            case ClassificationManagerService.ValidationResult.NAME_APPEARS_TWICE:
                result.link = [controller: 'TreeFixup', action: 'selectNameNode', params: result.params]
                break;

            case ClassificationManagerService.ValidationResult.CURRENT_NO_PARENT:
            case ClassificationManagerService.ValidationResult.NODE_HAS_MULTIPLE_SUPERNODES:
                result.link = [controller: 'TreeFixup', action: 'enddateAndMakeCurrent', params: result.params]
                break;

            default:
                break;
        }

        result.nested?.each { fixLinksFor(it) }

    }

    @RequiresRoles('admin')
    def clearValidationResults() {
        session[VALIDATION_RESULTS_KEY] = null
        redirect action: "index"
    }
}
