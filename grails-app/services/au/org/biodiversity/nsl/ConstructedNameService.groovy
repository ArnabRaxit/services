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

class ConstructedNameService {

    def classificationService
    static transactional = false

    public static String stripMarkUp(String string) {
        string?.replaceAll(/<[^>]*>/, '')?.replaceAll(/(&lsquo;|&rsquo;)/, "'")?.trim()
    }

    Map constructName(Name name) {
        constructName(name, null, 0)
    }

    Map constructName(Name name, Name parent) {
        constructName(name, parent, null, 0)
    }

    Map constructName(Name name, Integer nextRank, Integer count) {
        constructName(name, getFirstParentName(name), nextRank, count)
    }

    Map constructName(Name name, Name parent, Integer nextRank, Integer count) {

        if (!name) {
            throw new NullPointerException("Name can't be null.")
        }

        if (name.nameType?.scientific) {
            return constructScientificName(name, parent, nextRank, count)
        }
        if (name.nameType?.cultivar) {
            return constructCultivarName(name, parent)
        }
        if (name.nameType?.name == 'informal') {
            return constructInformalName(name, parent)
        }
        if (name.nameType?.name == 'common') {
            String markedUpName = "<common><name id='$name.id'><element>$name.nameElement</element></name></common>"
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
        }
        return [fullMarkedUpName: (name.nameElement ?: '?'), simpleMarkedUpName: (name.nameElement ?: '?')]
    }

    //join only non null bits with spaces
    private static String join(List<String> bits) {
        bits.findAll { it }.join(' ')
    }


    private Map constructInformalName(Name name, Name parent) {
        List<String> bits = []
        if (parent) {
            if (parent == name) {
                bits << '[recursive]'
            } else {
                bits << filterPrecedingName(constructName(parent).simpleMarkedUpName)
            }
        }
        bits << "<element>${name.nameElement}</element>"
        bits << constructAuthor(name)
        String markedUpName = "<informal><name id='$name.id'>${join(bits)}</name></informal>"
        return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
    }

    private Map constructCultivarName(Name name, Name parent) {
        List<String> bits = []
        if (parent) {
            if (parent == name) {
                bits << '[recursive]'
            } else {
                bits << filterPrecedingName(constructName(parent).simpleMarkedUpName)
            }

            bits << (name.nameType.connector) ? "<hybrid id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</hybrid>" : ''

            //NSL-605
            if (name.nameType.formula) {
                Name secondParent = name.secondParent
                if (secondParent && secondParent != name) {
                    bits << filterPrecedingName(constructName(secondParent).simpleMarkedUpName)
                }
            } else {
                bits << "<element>&lsquo;${name.nameElement}&rsquo;</element>"
            }
        } else {
            bits << "'<element>$name.nameElement</element>"
        }
        String markedUpName = "<cultivar><name id='$name.id'>${join(bits)}</name></cultivar>"
        return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
    }

    private Map constructScientificName(Name name, Name parent, Integer nextRankOrder, Integer count) {

        if (!nextRankOrder && name.nameRank) {
            if (name.nameRank.sortOrder == 500 && name.parent) {
                if(RankUtils.nameAtRankOrLower(name.parent, 'Genus')) {
                    nextRankOrder = RankUtils.getRankOrder('Genus')
                } else {
                    nextRankOrder = name.parent.nameRank.sortOrder
                }
            } else if (RankUtils.nameAtRankOrLower(name, 'Genus')) {
                nextRankOrder = RankUtils.getRankOrder('Genus')
            } else if (RankUtils.nameAtRankOrLower(name, 'Familia')) {
                nextRankOrder = RankUtils.getRankOrder('Familia')
            } else {
                nextRankOrder = name.nameRank.sortOrder
            }
        }

        Map precedingName = [:]
        if ((name.nameRank?.sortOrder > nextRankOrder) || (parent && name.nameType.formula)) {
            precedingName = getPrecedingName(name, parent, nextRankOrder, ++count)
        }

        String rank = makeRankString(parent, name)

        String connector = makeConnectorString(name, precedingName, rank)

        Boolean italic = RankUtils.rankLowerThan(name.nameRank, 'Genus') && !name.nameType.cultivar && name.nameType.name != 'phrase name'

        String el = "<element>${italic ? '<i>' : ''}$name.nameElement${italic ? '</i>' : ''}</element>"
        Map nameElement = [fullMarkedUpName: el, simpleMarkedUpName: el]

        if (parent && name.nameType.formula) {
            Name secondParent = name.secondParent
            if (secondParent && secondParent != name) {
                nameElement = constructName(secondParent, nextRankOrder, count)
            } else {
                el = "<element>?</element>"
                nameElement = [fullMarkedUpName: el, simpleMarkedUpName: el]
            }
        }

        String author = (!(name.nameType.formula || name.nameType.autonym)) ? constructAuthor(name) : ''

        String manuscript = (name.nameStatus.name == 'manuscript') ? '<manuscript>MS</manuscript>' : ''

        List<String> fullNameParts = [precedingName.fullMarkedUpName, rank, connector, nameElement.fullMarkedUpName, author, manuscript]
        List<String> simpleNameParts = [precedingName.simpleMarkedUpName, rank, connector, nameElement.simpleMarkedUpName, manuscript]

        String fullMarkedUpName = "<scientific><name id='$name.id'>${join(fullNameParts)}</name></scientific>"
        String simpleMarkedUpName = "<scientific><name id='$name.id'>${join(simpleNameParts)}</name></scientific>"
        return [fullMarkedUpName: fullMarkedUpName, simpleMarkedUpName: simpleMarkedUpName]
    }

    public String makeConnectorString(Name name, Map precedingName, String rank) {
        if ((name.nameType.connector) &&
                !(rank && name.nameType.connector == 'x' && name.nameRank.abbrev.startsWith('notho'))
        ) {
            return "<hybrid id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</hybrid>"
        } else {
            return ''
        }
    }

    public String makeRankString(Name parent, Name name) {
        if (parent && name.nameRank?.visibleInName && !name.nameType.formula) {
            if (name.nameRank.name == '[unranked]' && name.verbatimRank) {
                return "<rank id='${name.nameRank?.id}'>${name.verbatimRank}</rank>"
            }
            return "<rank id='${name.nameRank?.id}'>${name.nameRank?.abbrev}</rank>"
        }
        return ''
    }

    private Map getPrecedingName(Name name, Name parent, Integer nextRank, Integer count) {
        if (parent && (parent.nameRank.sortOrder >= nextRank)) {
            //mumble mumble
            if (parent == name || count == 5) {
                log.error "parent $parent of name $name is recursive (count $count)"
                return [fullMarkedUpName: '[recursive]', simpleMarkedUpName: '[recursive]']
            } else {
                if (parent.nameType.scientific) {
                    Map precedingNames = constructName(parent, nextRank, count)

                    String fullPrecedingName = filterPrecedingName(precedingNames.fullMarkedUpName)
                    String simplePrecedingName = filterPrecedingName(precedingNames.simpleMarkedUpName)

                    if (!(name.nameType.formula || name.nameType.autonym)) {
                        fullPrecedingName = simplePrecedingName
                    }

                    if (parent.nameType.formula && name.nameType.formula) {
                        return [fullMarkedUpName: "($fullPrecedingName)", simpleMarkedUpName: "($simplePrecedingName)"]
                    }
                    return [fullMarkedUpName: fullPrecedingName, simpleMarkedUpName: simplePrecedingName]
                } else {
                    log.error "parent $parent of name $name is not scientific"
                    return [fullMarkedUpName: '[non-scientific name]', simpleMarkedUpName: '[non-scientific name]']
                }
            }
        }
        return [fullMarkedUpName: '', simpleMarkedUpName: '']
    }

    private static String filterPrecedingName(String string) {
        string.replaceAll(/ (<manuscript>MS<\/manuscript>)/, '')
    }

    public String getAuthorityFromFullNameHTML(String fullName) {
        stripMarkUp(fullName.replaceAll(/^.*<authors>(.*)<\/authors>.*$/, '$1'))
    }

    String constructAuthor(Name name) {
        List<String> bits = []
        if (name.author) {
            if (name.baseAuthor) {
                if (name.exBaseAuthor) {
                    bits << "(<ex-base id='$name.exBaseAuthor.id' title='${name.exBaseAuthor.name.encodeAsHTML()}'>$name.exBaseAuthor.abbrev</ex-base> ex <base id='$name.baseAuthor.id' title='${name.baseAuthor.name.encodeAsHTML()}'>$name.baseAuthor.abbrev</base>)"
                } else {
                    bits << "(<base id='$name.baseAuthor.id' title='${name.baseAuthor.name.encodeAsHTML()}'>$name.baseAuthor.abbrev</base>)"
                }
            }
            if (name.exAuthor) {
                bits << "<ex id='$name.exAuthor.id' title='${name.exAuthor.name.encodeAsHTML()}'>$name.exAuthor.abbrev</ex> ex"
            }
            bits << "<author id='$name.author.id' title='${name.author.name.encodeAsHTML()}'>$name.author.abbrev</author>"
            if (name.sanctioningAuthor) {
                bits << ": <sanctioning id='$name.sanctioningAuthor.id' title='${name.sanctioningAuthor.name.encodeAsHTML()}'>$name.sanctioningAuthor.abbrev</sanctioning>"
            }
        }
        return bits.size() ? "<authors>${join(bits)}</authors>" : ''
    }

    Name getFirstParentName(Name name) {
        if (name.nameType.formula) {
            return name.parent
        }
        if (name.nameRank.hasParent && name.parent) {
            Name parent = (name.parent.nameRank == name.nameRank && name.parent.parent) ? name.parent.parent : name.parent

            //NSL-856 cultivar hybrid display genus + epithet
            if (name.nameType.hybrid && name.nameType.cultivar) {
                if (name.parent == parent) {
                    return RankUtils.getParentOfRank(name, 'Genus')
                }
                return RankUtils.getParentOfRank(name.parent, 'Genus')
            }
            //NSL-927 cultivar display to lowest parent rank
            if (!name.nameType.hybrid && name.nameType.cultivar) {
                return name.parent
            }
            //just a scientific name
            return findNameParent(parent)
        }
        return null
    }

    /**
     * Find the "name parent" from a names direct parent
     * @param parent the direct parent of the name
     * @return the next major ranked parent below or equal to Genus or NULL
     */
    private Name findNameParent(Name parent) {
        if (parent.nameRank.major) {
            return parent
        }
        NameTreePath nameTreePath = NameTreePathService.findCurrentNameTreePath(parent, 'APNI')
        if (nameTreePath) {
            List<Name> namesInBranch = nameTreePath.namesInBranch()
            return namesInBranch.reverse().find { it.nameRank.major }
        } else {
            log.error "no name tree path for $parent checking tree"
            List<Name> namesInBranch = classificationService.getPath(parent)
            //todo make name tree paths ?
            if (namesInBranch) {
                return namesInBranch.reverse().find {
                    if (it) {
                        it.nameRank.major
                    } else {
                        log.debug namesInBranch
                    }
                }
            }
            return null
        }
    }
}
