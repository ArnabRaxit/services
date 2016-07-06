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

package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.CsvRenderer
import au.org.biodiversity.nsl.NameTagName
import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.UriNs
import grails.converters.JSON
import grails.converters.XML
import org.apache.shiro.SecurityUtils

import javax.servlet.http.Cookie

class SearchController {
    def configService
    def searchService

    def search(Integer max) {
        String referer = request.getHeader('Referer')
        log.info "Search params $params, Referer: ${referer}"
        max = max ?: 100;

        if (!params.product && !SecurityUtils.subject?.authenticated) {
            params.product = params.display ?: 'apni'
        }

        if (params.product) {
            Arrangement tree = Arrangement.findByNamespaceAndLabel(configService.nameSpace, params.product.toUpperCase() as String)
            if (tree) {
                params.tree = [id: tree.id]
                params.display = params.product
            } else {
                flash.message = "Unknown product ${params.product}"
                return redirect(url: '/')
            }
        } else {
            params.display = params.display ?: 'apni'
        }


        Map incMap = searchService.checked(params, 'inc')

        if (incMap.isEmpty() && params.search != 'true' && params.advanced != 'true' && params.nameCheck != 'true') {
            String inc = g.cookie(name: 'searchInclude')
            if (inc) {
                incMap = JSON.parse(inc) as Map
            } else {
                incMap = [scientific: 'on']
            }
        }

        params.inc = incMap

        Cookie incCookie = new Cookie("searchInclude", (incMap as JSON).toString())
        incCookie.maxAge = 3600 //1 hour
        response.addCookie(incCookie)

        List displayFormats = ['apni', 'apc']
        if (params.search == 'true' || params.advanced == 'true' || params.nameCheck == 'true') {
            log.debug "doing search"
            Map results = searchService.searchForName(params, max)
            List models = results.names
            if (results.message) {
                flash.message = results.message
            }
            withFormat {
                html {
                    return render(view: 'search',
                            model: [names         : models,
                                    query         : params,
                                    count         : results.count,
                                    total         : results.total,
                                    max           : max,
                                    displayFormats: displayFormats])
                }
                json {
                    return render(contentType: 'application/json') { models }
                }
                xml {
                    return render(models as XML)
                }
            }
        }


        if (params.sparql && !params.product) {
            // we do not do the searching here, instead we re-render the page in sparql mode
            // when the page is re-rendered like this, it will fire off its client-side search.
            log.debug "re-rendering in sparql mode"

            Map uriPrefixes = [
                    'http://www.w3.org/2001/XMLSchema#'                   : 'xs',
                    'http://www.w3.org/1999/02/22-rdf-syntax-ns#'         : 'rdf',
                    'http://www.w3.org/2000/01/rdf-schema#'               : 'rdfs',
                    'http://www.w3.org/2002/07/owl#'                      : 'owl',
                    'http://purl.org/dc/elements/1.1/'                    : 'dc',
                    'http://purl.org/dc/terms/'                           : 'dcterms',
                    'http://rs.tdwg.org/ontology/voc/TaxonName#'          : 'tdwg_tn',
                    'http://rs.tdwg.org/ontology/voc/TaxonConcept#'       : 'tdwg_tc',
                    'http://rs.tdwg.org/ontology/voc/PublicationCitation#': 'tdwg_pc',
                    'http://rs.tdwg.org/ontology/voc/Common#'             : 'tdwg_comm',
                    'http://biodiversity.org.au/voc/ibis/IBIS#'           : 'ibis',
                    'http://biodiversity.org.au/voc/afd/AFD#'             : 'afd',
                    'http://biodiversity.org.au/voc/apni/APNI#'           : 'apni',
                    'http://biodiversity.org.au/voc/apc/APC#'             : 'apc',
                    'http://biodiversity.org.au/voc/afd/profile#'         : 'afd_prf',
                    'http://biodiversity.org.au/voc/apni/profile#'        : 'apni_prf',
                    'http://biodiversity.org.au/voc/graph/GRAPH#'         : 'g',
                    'http://creativecommons.org/ns#'                      : 'cc',
                    'http://biodiversity.org.au/voc/boa/BOA#'             : 'boa',
                    'http://biodiversity.org.au/voc/boa/Name#'            : 'boa-name',
                    'http://biodiversity.org.au/voc/boa/Tree#'            : 'boa-tree',
                    'http://biodiversity.org.au/voc/boa/Instance#'        : 'boa-inst',
                    'http://biodiversity.org.au/voc/boa/Reference#'       : 'boa-ref',
                    'http://biodiversity.org.au/voc/boa/Author#'          : 'boa-auth',
                    'http://biodiversity.org.au/voc/nsl/NSL#'             : 'nsl',
                    'http://biodiversity.org.au/voc/nsl/Tree#'            : 'nsl-tree',
                    'http://biodiversity.org.au/voc/nsl/APC#'             : 'nsl-apc',
                    'http://biodiversity.org.au/voc/nsl/Namespace#'       : 'nsl-ns'
            ];

            // if we have an item in UriNs, then it will override these handy prefixes.

            UriNs.all.each { uriPrefixes.put(it.uri, it.label) }

            return [query: params, max: max, displayFormats: displayFormats, uriPrefixes: uriPrefixes, stats: stats]
        }

        return [query: params, max: max, displayFormats: displayFormats]

    }

    def searchForm() {
        if (!params.product && !SecurityUtils.subject?.authenticated) {
            params.product = params.display ?: 'apni'
        }

        if (params.product) {
            Arrangement tree = Arrangement.findByNamespaceAndLabel(configService.nameSpace, params.product.toUpperCase() as String)
            if (tree) {
                params.tree = [id: tree.id]
                params.display = params.product
            } else {
                flash.message = "Unknown product ${params.product}"
                return redirect(url: '/')
            }
        } else {
            params.display = params.display ?: 'apni'
        }


        render([template: 'advanced-search-form', model: [query: params, max: 100]])
    }

    def nameCheck(Integer max) {
        List<Map> results = searchService.nameCheck(params, max)
        params.product = 'apni'
        if (params.csv) {
            render(file: renderCsvResults(results).bytes, contentType: 'text/csv', fileName: 'name-check.csv')
        } else {
            render(view: 'search', model: [results: results, query: params, max: max])
        }
    }

    private static String renderCsvResults(List<Map> results) {
        List<List> csvResults = []
        results.each { Map result ->
            if (result.names.empty) {
                csvResults.add([result.found,
                                result.query,
                                '',
                                'not found',
                                '',
                                '',
                                ''
                ])
            } else {
                result.names.each { Map nameData ->
                    csvResults.add([result.found,
                                    result.query,
                                    apcStatus(nameData.apc),
                                    nameData.name.fullName,
                                    nameData.name.nameStatus.name,
                                    nameData.name.nameType.name,
                                    (nameData.name.tags.collect { NameTagName tag -> tag.tag.name }).toString()
                    ])
                }
            }
        }
        return CsvRenderer.renderAsCsv(['Found?', 'Search term', 'Census', 'Matched name(s)', 'Name status','Name type','Tags'], csvResults)
    }

    private static String apcStatus(Node node) {
        if (node) {
            if (node.typeUriIdPart == 'ApcConcept') {
                return 'APC'
            } else {
                return 'APC Excluded'
            }
        }
        return '-'
    }

}
