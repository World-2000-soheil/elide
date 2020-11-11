/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@TestInstance(Lifecycle.PER_CLASS)
public class HandlebarsHydratorTest {

    private static final String CONFIG_PATH = "src/test/resources/validator/valid";

    private static final String VALID_TABLE_WITH_VARIABLES = "{\n"
            + "  tables: [{\n"
            + "      name: <% name %>\n"
            + "      table: <% table %>\n"
            + "      schema: gamedb\n"
            + "      description:\n"
            + "      // newlines are replaced by single space in handlebar if no helper function is applied\n"
            + "      '''\n"
            + "      A long description\n"
            + "      with newline\n"
            + "       and additional space at start of this line.\n"
            + "      '''\n"
            + "      category: Table Category\n"
            + "      cardinality : lARge\n"
            + "      hidden : false\n"
            + "      readAccess : A user is admin or is a player in the game\n"
            + "      filterTemplate : countryIsoCode=={{code}}\n"
            + "      tags: ['GAME', 'PLAYER',  '''\n"
            + "      A tag\n"
            + "      with newline\n"
            + "      ''']\n"
            + "      joins: [\n"
            + "          {\n"
            + "             name: playerCountry\n"
            + "             to: Country\n"
            + "             type: toOnE\n"
            + "             definition: '{{ playerCountry.id}} = {{country_id}}'\n"
            + "          },\n"
            + "          {\n"
            + "             name: playerTeam\n"
            + "             to: Team\n"
            + "             type: Tomany\n"
            + "             definition: '{{playerTeam.id}} = {{ team_id}}'\n"
            + "          }\n"
            + "      ]\n"
            + "\n"
            + "      measures : [\n"
            + "          {\n"
            + "          name : highScore\n"
            + "          type : INteGER\n"
            + "          description : very awesome score\n"
            + "          definition: 'MAX({{score}})'\n"
            + "          tags: ['PUBLIC']\n"
            + "          }\n"
            + "      ]\n"
            + "      dimensions : [\n"
            + "         {\n"
            + "           name : countryIsoCode\n"
            + "           type : TEXT\n"
            + "           category : country detail\n"
            + "           definition : '{{playerCountry.isoCode}}'\n"
            + "           values : ['US', 'HK']\n"
            + "           tags: ['PRIVATE']\n"
            + "           cardinality: Small\n"
            + "         },\n"
            + "         {\n"
            + "           name : createdOn\n"
            + "           type : TIME\n"
            + "           definition : '{{create_on}}'\n"
            + "           grain:\n"
            + "            {\n"
            + "             type : DaY\n"
            + "             sql :  '''\n"
            + "             PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n"
            + "             '''\n"
            + "            }\n"
            + "         },\n"
            + "         {\n"
            + "           name : updatedOn\n"
            + "           type : TIme\n"
            + "           definition : '{{updated_on}}'\n"
            + "         }\n"
            + "      ]\n"
            + "  }]\n"
            + "}\n";

    private static final String VALID_TABLE_JAVA_NAME = "PlayerStats";

    private static final String VALID_TABLE_CLASS = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.Include;\n"
            + "import com.yahoo.elide.annotation.Exclude;\n"
            + "import com.yahoo.elide.annotation.ReadPermission;\n"
            + "import com.yahoo.elide.annotation.UpdatePermission;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Join;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Temporal;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;\n"
            + "import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;\n"
            + "\n"
            + "import lombok.Data;\n"
            + "import lombok.EqualsAndHashCode;\n"
            + "import lombok.ToString;\n"
            + "\n"
            + "import java.math.BigDecimal;\n"
            + "import java.util.Set;\n"
            + "import javax.persistence.Id;\n"
            + "\n"
            + "\n"
            + "\n"
            + "/**\n"
            + " * A root level entity for testing AggregationDataStore.\n"
            + " */\n"
            + "@EqualsAndHashCode\n"
            + "@ToString\n"
            + "@Data\n"
            + "@FromTable(name = \"gamedb.player_stats\", dbConnectionName = \"\")\n"
            + "\n"
            + "@ReadPermission(expression = \"A user is admin or is a player in the game\")\n"
            + "@TableMeta(\n"
            + "    size = CardinalitySize.LARGE,\n"
            + "    description = \"A long description with newline  and additional space at start of this line.\",\n"
            + "    category=\"Table Category\",\n"
            + "    tags={\"GAME\",\"PLAYER\",\"A tag with newline\"},\n"
            + "    filterTemplate=\"countryIsoCode=={{code}}\",\n"
            + "    isFact=true\n"
            + ")\n"
            + "@Include(type = \"playerStats\")\n"
            + "public class PlayerStats {\n"
            + "\n"
            + "    private String id;\n"
            + "\n"
            + "    @Id\n"
            + "    public String getId() {\n"
            + "        return id;\n"
            + "    }\n"
            + "\n"
            + "    public void setId(String id) {\n"
            + "        this.id = id;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    protected String countryIsoCode;\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        size = CardinalitySize.SMALL,\n"
            + "        description = \"countryIsoCode\",\n"
            + "        category=\"country detail\",\n"
            + "        values={\"US\",\"HK\"},\n"
            + "        tags={\"PRIVATE\"},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    @DimensionFormula(\"{{playerCountry.isoCode}}\")\n"
            + "\n"
            + "    public String getCountryIsoCode() {\n"
            + "        return countryIsoCode;\n"
            + "    }\n"
            + "\n"
            + "    public void setCountryIsoCode(String countryIsoCode) {\n"
            + "        this.countryIsoCode = countryIsoCode;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    protected com.yahoo.elide.datastores.aggregation.timegrains.Day createdOn;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(grain = TimeGrain.DAY, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\"), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"createdOn\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    @DimensionFormula(\"{{create_on}}\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.Day getCreatedOn() {\n"
            + "        return createdOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedOn(com.yahoo.elide.datastores.aggregation.timegrains.Day createdOn) {\n"
            + "        this.createdOn = createdOn;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    protected com.yahoo.elide.datastores.aggregation.timegrains.Day updatedOn;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"updatedOn\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    @DimensionFormula(\"{{updated_on}}\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.Day getUpdatedOn() {\n"
            + "        return updatedOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setUpdatedOn(com.yahoo.elide.datastores.aggregation.timegrains.Day updatedOn) {\n"
            + "        this.updatedOn = updatedOn;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Country playerCountry;\n"
            + "\n"
            + "    @Join(\"{{playerCountry.id}} = {{country_id}}\")\n"
            + "    public Country getPlayerCountry() {\n"
            + "        return playerCountry;\n"
            + "    }\n"
            + "\n"
            + "    public void setPlayerCountry(Country playerCountry) {\n"
            + "        this.playerCountry = playerCountry;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Set<Team> playerTeam;\n"
            + "\n"
            + "    @Join(\"{{playerTeam.id}} = {{team_id}}\")\n"
            + "    public Set<Team> getPlayerTeam() {\n"
            + "        return playerTeam;\n"
            + "    }\n"
            + "\n"
            + "    public void setPlayerTeam(Set<Team> playerTeam) {\n"
            + "        this.playerTeam = playerTeam;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    \n"
            + "    protected Long highScore;\n"
            + "    \n"
            + "    @MetricFormula(value=\"MAX({{score}})\")\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        description = \"very awesome score\",\n"
            + "        category=\"\",\n"
            + "        tags={\"PUBLIC\"}\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    public Long getHighScore() {\n"
            + "        return highScore;\n"
            + "    }\n"
            + "\n"
            + "    public void setHighScore(Long highScore) {\n"
            + "        this.highScore = highScore;\n"
            + "    }\n"
            + "\n"
            + "}\n";

    private static final String VALID_SECURITY_ADMIN_JAVA_NAME = "DynamicConfigOperationChecksPrincipalIsAdmin";
    private static final String VALID_SECURITY_GUEST_JAVA_NAME = "DynamicConfigOperationChecksPrincipalIsGuest";

    private static final String VALID_SECURITY_ADMIN_JAVA = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "\n"
            + "@SecurityCheck(DynamicConfigOperationChecksPrincipalIsAdmin.PRINCIPAL_IS_ADMIN)\n"
            + "public class DynamicConfigOperationChecksPrincipalIsAdmin extends RoleMemberCheck {\n"
            + "\n"
            + "    public static final String PRINCIPAL_IS_ADMIN = \"Principal is admin\";\n"
            + "    public DynamicConfigOperationChecksPrincipalIsAdmin() {\n"
            + "        super(\"admin\");\n"
            + "    }\n"
            + "}\n";

    private static final String VALID_SECURITY_GUEST_JAVA = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "\n"
            + "@SecurityCheck(DynamicConfigOperationChecksPrincipalIsGuest.PRINCIPAL_IS_GUEST)\n"
            + "public class DynamicConfigOperationChecksPrincipalIsGuest extends RoleMemberCheck {\n"
            + "\n"
            + "    public static final String PRINCIPAL_IS_GUEST = \"Principal is guest\";\n"
            + "    public DynamicConfigOperationChecksPrincipalIsGuest() {\n"
            + "        super(\"guest\");\n"
            + "    }\n"
            + "}\n";

    private static final String VALID_CHILD_TABLE_CLASS = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.Include;\n"
            + "import com.yahoo.elide.annotation.Exclude;\n"
            + "import com.yahoo.elide.annotation.ReadPermission;\n"
            + "import com.yahoo.elide.annotation.UpdatePermission;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Join;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Temporal;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;\n"
            + "import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;\n"
            + "\n"
            + "import lombok.Data;\n"
            + "import lombok.EqualsAndHashCode;\n"
            + "import lombok.ToString;\n"
            + "\n"
            + "import java.math.BigDecimal;\n"
            + "import java.util.Set;\n"
            + "import javax.persistence.Id;\n"
            + "\n"
            + "\n"
            + "\n"
            + "/**\n"
            + " * A root level entity for testing AggregationDataStore.\n"
            + " */\n"
            + "@EqualsAndHashCode\n"
            + "@ToString\n"
            + "@Data\n"
            + "\n"
            + "@ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "@TableMeta(\n"
            + "    size = CardinalitySize.LARGE,\n"
            + "    description = \"PlayerStats Child\",\n"
            + "    category=\"\",\n"
            + "    tags={},\n"
            + "    filterTemplate=\"\",\n"
            + "    isFact=true\n"
            + ")\n"
            + "@Include(type = \"playerStatsChild\")\n"
            + "public class PlayerStatsChild extends PlayerStats{\n"
            + "\n"
            + "    private String id;\n"
            + "\n"
            + "    @Id\n"
            + "    public String getId() {\n"
            + "        return id;\n"
            + "    }\n"
            + "\n"
            + "    public void setId(String id) {\n"
            + "        this.id = id;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(grain = TimeGrain.DAY, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\"), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"createdOn\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    @Override\n"
            + "    @DimensionFormula(\"{{create_on}}\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.Day getCreatedOn() {\n"
            + "        return createdOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedOn(com.yahoo.elide.datastores.aggregation.timegrains.Day createdOn) {\n"
            + "        this.createdOn = createdOn;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    protected com.yahoo.elide.datastores.aggregation.timegrains.Year createdYear;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(grain = TimeGrain.YEAR, expression = \"PARSEDATETIME(FORMATDATETIME(createdOn, 'yyyy-MM-dd'), 'yyyy')\"), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"createdYear\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    @DimensionFormula(\"{{createdYear}}\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.Year getCreatedYear() {\n"
            + "        return createdYear;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedYear(com.yahoo.elide.datastores.aggregation.timegrains.Year createdYear) {\n"
            + "        this.createdYear = createdYear;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    protected com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek createdWeekDate;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(grain = TimeGrain.ISOWEEK, expression = \"PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\"), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"createdWeekDate\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    @DimensionFormula(\"{{createdOn}}\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek getCreatedWeekDate() {\n"
            + "        return createdWeekDate;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedWeekDate(com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek createdWeekDate) {\n"
            + "        this.createdWeekDate = createdWeekDate;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    protected com.yahoo.elide.datastores.aggregation.timegrains.Month updatedMonth;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyyMM')\"), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"updatedMonth\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    @DimensionFormula(\"{{updated_month}}\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.Month getUpdatedMonth() {\n"
            + "        return updatedMonth;\n"
            + "    }\n"
            + "\n"
            + "    public void setUpdatedMonth(com.yahoo.elide.datastores.aggregation.timegrains.Month updatedMonth) {\n"
            + "        this.updatedMonth = updatedMonth;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    \n"
            + "    \n"
            + "    @MetricFormula(value=\"MAX({{score}})\", queryPlan=com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanResolver.class)\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        description = \"very awesome score\",\n"
            + "        category=\"\",\n"
            + "        tags={\"PUBLIC\"}\n"
            + "    )\n"
            + "    \n"
            + "    @Override\n"
            + "    public Long getHighScore() {\n"
            + "        return highScore;\n"
            + "    }\n"
            + "\n"
            + "    public void setHighScore(Long highScore) {\n"
            + "        this.highScore = highScore;\n"
            + "    }\n"
            + "\n"
            + "    \n"
            + "    protected Long AvgScore;\n"
            + "    \n"
            + "    @MetricFormula(value=\"Avg({{score}})\")\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        description = \"Avg score\",\n"
            + "        category=\"\",\n"
            + "        tags={\"PUBLIC\"}\n"
            + "    )\n"
            + "    \n"
            + "    \n"
            + "    public Long getAvgScore() {\n"
            + "        return AvgScore;\n"
            + "    }\n"
            + "\n"
            + "    public void setAvgScore(Long AvgScore) {\n"
            + "        this.AvgScore = AvgScore;\n"
            + "    }\n"
            + "\n"
            + "}\n";

    private DynamicConfigValidator testClass;
    private HandlebarsHydrator hydrator;

    @BeforeAll
    public void setup() throws IOException {
        hydrator = new HandlebarsHydrator();
        testClass = new DynamicConfigValidator(CONFIG_PATH);
        testClass.readAndValidateConfigs();
    }

    @Test
    public void testConfigHydration() throws IOException {
        File file = new File(CONFIG_PATH);
        String hjsonPath = file.getAbsolutePath() + "/models/tables/player_stats.hjson";
        String content = new String (Files.readAllBytes(Paths.get(hjsonPath)));

        assertEquals(content, hydrator.hydrateConfigTemplate(
                VALID_TABLE_WITH_VARIABLES, testClass.getModelVariables()));
    }
    @Test
    public void testTableHydration() throws IOException {
        Map<String, String> tableClasses = hydrator.hydrateTableTemplate(testClass.getElideTableConfig());

        assertTrue(tableClasses.keySet().contains(VALID_TABLE_JAVA_NAME));
        assertEquals(VALID_TABLE_CLASS, tableClasses.get(VALID_TABLE_JAVA_NAME));
    }

    @Test
    public void testChildTableHydration() throws IOException {
        Map<String, String> tableClasses = hydrator.hydrateTableTemplate(testClass.getElideTableConfig());

        assertTrue(tableClasses.keySet().contains("PlayerStatsChild"));
        assertEquals(VALID_CHILD_TABLE_CLASS, tableClasses.get("PlayerStatsChild"));
    }

    @Test
    public void testSecurityHydration() throws IOException {
        Map<String, String> securityClasses = hydrator.hydrateSecurityTemplate(testClass.getElideSecurityConfig());

        assertEquals(true, securityClasses.keySet().contains(VALID_SECURITY_ADMIN_JAVA_NAME));
        assertEquals(true, securityClasses.keySet().contains(VALID_SECURITY_GUEST_JAVA_NAME));
        assertEquals(VALID_SECURITY_ADMIN_JAVA, securityClasses.get(VALID_SECURITY_ADMIN_JAVA_NAME));
        assertEquals(VALID_SECURITY_GUEST_JAVA, securityClasses.get(VALID_SECURITY_GUEST_JAVA_NAME));
    }
}
