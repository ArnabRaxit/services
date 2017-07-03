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
/**
 * User: pmcneil
 * Date: 3/12/14
 *
 */
class RankUtils {


    private static Integer rankOrder(String rankName){
        NameRank.findByName(rankName).sortOrder
    }
    
    /**
     * Checks if the NameRank provided is higher than the rank of Name rankName.
     * Higher ranks have a lower rank sort order, so Genus is higher that Species.
     *
     * assert rankHigherThan(NameRank.findByName('Genus'), 'Species') == true
     * assert rankHigherThan(NameRank.findByName('Species'), 'Genus') == false
     * assert rankHigherThan(NameRank.findByName('Genus'), 'Genus') == false
     *
     * @param rank
     * @param rankName
     * @return true if rank is higher than the rank with the name rankName
     */
    static Boolean rankHigherThan(NameRank rank, String rankName) {
        return rankOrder(rankName) > rank.sortOrder
    }

    /**
     * Checks if the NameRank provided is lower than the rank of Name rankName.
     * Lower ranks have a higher rank sort order, so Species is lower that Genus.
     *
     * assert rankLowerThan(NameRank.findByName('Genus'), 'Species') == false
     * assert rankLowerThan(NameRank.findByName('Species'), 'Genus') == true
     *
     * This ranks with a sort order of 500 (or above) will always return false
     * as they are effectively unranked.
     *
     * @param rank
     * @param rankName
     * @return true if rank is lower than the rank with the name rankName
     */
    static Boolean rankLowerThan(NameRank rank, String rankName) {
        return rank.sortOrder < 500 && rankOrder(rankName) < rank.sortOrder
    }

    static Boolean nameAtRankOrHigher(Name name, String rankName) {
        return rankOrder(rankName) >= name.nameRank.sortOrder
    }

    static Boolean nameAtRankOrLower(Name name, String rankName) {
        return rankOrder(rankName) <= name.nameRank.sortOrder
    }

    static Boolean nameLowerThanRank(Name name, String rankName) {
        return rankOrder(rankName) < name.nameRank.sortOrder
    }

    static Boolean nameHigherThanRank(Name name, String rankName) {
        return rankOrder(rankName) > name.nameRank.sortOrder
    }

    static Integer getRankOrder(String rankName) {
        rankOrder(rankName)
    }

    static Name getParentOfRank(Name name, String rank) {
        getParentOfRank(name, rank, 'APNI')
    }

    static Name getParentOfRank(Name name, String rank, String treeName) {
        if(name.parent?.nameRank?.name == rank) {
            return name.parent
        }
        NameTreePath nameTreePath = NameTreePathService.findCurrentNameTreePath(name, treeName)
        if (nameTreePath) {
            List<Name> namesInBranch = nameTreePath.namesInBranch()
            return namesInBranch.reverse().find { (it && it.nameRank.name == rank) }
        }
        return null
    }

    static Name getFamily(Name name, String treeName) {
        NameTreePath nameTreePath = NameTreePathService.findCurrentNameTreePath(name, treeName)
        return nameTreePath.family
    }

    static Map<String, Name> getFamilyByTrees(Name name) {
        List<NameTreePath> paths = NameTreePath.findAllByName(name)
        Map familyTreeMap = [:]
        paths.each {NameTreePath nameTreePath ->
            familyTreeMap.put(nameTreePath.tree.label,  nameTreePath.family)
        }
        return familyTreeMap
    }

    static previousRank(NameRank rank) {
        return NameRank.findBySortOrderLessThan(rank.sortOrder, [sort: 'sortOrder', order: 'desc'])
    }

    static nextMajorRank(NameRank rank, Boolean up = true) {
        return NameRank.findBySortOrderLessThanAndMajor(rank.sortOrder, true, [sort: 'sortOrder', order: up ? 'desc' : 'asc'])
    }

    static parentOrHigher(NameRank rank) {
        rank.parentRank ?: previousRank(rank)
    }

    static parentOrMajor(NameRank rank) {
        rank.parentRank ?: nextMajorRank(rank, true)
    }

    static isRankedHigherThan(Name a, Name b) {
        a.nameRank.sortOrder < b.nameRank.sortOrder
    }

    static isRankedLowerThan(Name a, Name b) {
        !isRankedHigherThan(a, b)
    }

    static isRankedEqualTo(Name a, Name b) {
        a.nameRank.sortOrder == b.nameRank.sortOrder
    }

}
