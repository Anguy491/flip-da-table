package com.flip.backend.conquerwesteros.engine;

import com.flip.backend.conquerwesteros.entities.BattleLine;
import com.flip.backend.conquerwesteros.entities.DieFace;
import com.flip.backend.conquerwesteros.entities.StrongholdCard;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConquerWesterosCatalog {
    public static final String STEAL_CROWN_LINE_ID = "STEAL_CROWN";
    public static final BattleLine STEAL_CROWN_LINE = symbols(STEAL_CROWN_LINE_ID, DieFace.CROWN);

    public record CampaignData(
            Campaign campaign,
            List<StrongholdCard> strongholds,
            Map<String, Integer> clanScores
    ) {
        public CampaignData {
            strongholds = List.copyOf(strongholds);
            clanScores = Map.copyOf(clanScores);
        }

        public StrongholdCard stronghold(String id) {
            return strongholds.stream().filter(card -> card.id().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown stronghold: " + id));
        }

        public List<String> clanStrongholds(String clan) {
            return strongholds.stream().filter(card -> card.clan().equals(clan)).map(StrongholdCard::id).toList();
        }

        public int clanScore(String clan) {
            Integer score = clanScores.get(clan);
            if (score == null) throw new IllegalArgumentException("unknown clan: " + clan);
            return score;
        }
    }

    private record Template(int points, List<BattleLine> lines) {}
    private record Mapping(TemplateId template, String name, String clan, boolean kingsLanding) {}

    private static final Map<TemplateId, Template> TEMPLATES = templates();
    private static final Map<Campaign, CampaignData> CAMPAIGNS = campaigns();

    private ConquerWesterosCatalog() {}

    public static CampaignData campaign(Campaign campaign) {
        return Objects.requireNonNull(CAMPAIGNS.get(campaign), "campaign");
    }

    public static Map<TemplateId, Integer> templatePoints() {
        var result = new EnumMap<TemplateId, Integer>(TemplateId.class);
        TEMPLATES.forEach((id, template) -> result.put(id, template.points()));
        return Map.copyOf(result);
    }

    private static Map<TemplateId, Template> templates() {
        var result = new EnumMap<TemplateId, Template>(TemplateId.class);
        result.put(TemplateId.T01, template(1, military("L1", 5)));
        result.put(TemplateId.T02, template(1, military("L1", 7)));
        result.put(TemplateId.T03, template(1, symbols("L1", DieFace.RAVEN, DieFace.KNIGHT)));
        result.put(TemplateId.T04, template(1, symbols("L1", DieFace.CROWN, DieFace.KNIGHT)));
        result.put(TemplateId.T05, template(2, military("L1", 5), symbols("L2", DieFace.RAVEN, DieFace.KNIGHT)));
        result.put(TemplateId.T06, template(2, military("L1", 3), symbols("L2", DieFace.RAVEN, DieFace.RAVEN)));
        result.put(TemplateId.T07, template(2, military("L1", 3), symbols("L2", DieFace.KNIGHT, DieFace.KNIGHT)));
        result.put(TemplateId.T08, template(2, military("L1", 8), symbols("L2", DieFace.CROWN)));
        result.put(TemplateId.T09, template(2, military("L1", 2), symbols("L2", DieFace.RAVEN, DieFace.RAVEN), symbols("L3", DieFace.KNIGHT)));
        result.put(TemplateId.T10, template(2, military("L1", 4), symbols("L2", DieFace.RAVEN, DieFace.KNIGHT), symbols("L3", DieFace.CROWN)));
        result.put(TemplateId.T11, template(3, military("L1", 6), symbols("L2", DieFace.RAVEN, DieFace.KNIGHT)));
        result.put(TemplateId.T12, template(3, military("L1", 6), symbols("L2", DieFace.RAVEN, DieFace.RAVEN)));
        result.put(TemplateId.T13, template(3, military("L1", 5), symbols("L2", DieFace.RAVEN, DieFace.RAVEN), symbols("L3", DieFace.KNIGHT)));
        result.put(TemplateId.T14, template(4, military("L1", 6), symbols("L2", DieFace.RAVEN, DieFace.RAVEN), symbols("L3", DieFace.KNIGHT)));
        return Map.copyOf(result);
    }

    private static Map<Campaign, CampaignData> campaigns() {
        var result = new EnumMap<Campaign, CampaignData>(Campaign.class);
        result.put(Campaign.WAR_OF_FIVE_KINGS, build(
                Campaign.WAR_OF_FIVE_KINGS,
                List.of(
                        map(TemplateId.T01, "White Harbor", "Stark–Tully Alliance"),
                        map(TemplateId.T02, "Moat Cailin", "Stark–Tully Alliance"),
                        map(TemplateId.T03, "Harrenhal", "Lannister Royalists"),
                        map(TemplateId.T04, "Ten Towers", "Greyjoy"),
                        map(TemplateId.T05, "Highgarden", "Tyrell"),
                        map(TemplateId.T06, "Riverrun", "Stark–Tully Alliance"),
                        map(TemplateId.T07, "Pyke", "Greyjoy"),
                        map(TemplateId.T08, "Dragonstone", "Baratheon"),
                        map(TemplateId.T09, "Oldtown", "Tyrell"),
                        map(TemplateId.T10, "King's Landing", "Lannister Royalists", true),
                        map(TemplateId.T11, "Winterfell", "Stark–Tully Alliance"),
                        map(TemplateId.T12, "Casterly Rock", "Lannister Royalists"),
                        map(TemplateId.T13, "The Eyrie", "Arryn"),
                        map(TemplateId.T14, "Storm's End", "Baratheon")
                ),
                clanScores(
                        "Stark–Tully Alliance", 10,
                        "Lannister Royalists", 8,
                        "Baratheon", 7,
                        "Tyrell", 5,
                        "Greyjoy", 4,
                        "Arryn", 3
                )
        ));
        result.put(Campaign.DANCE_OF_THE_DRAGONS, build(
                Campaign.DANCE_OF_THE_DRAGONS,
                List.of(
                        map(TemplateId.T01, "The Eyrie", "Blacks · Targaryen"),
                        map(TemplateId.T02, "Maidenpool", "Blacks · Targaryen"),
                        map(TemplateId.T03, "Storm's End", "Greens · Targaryen"),
                        map(TemplateId.T04, "Lannisport", "Lannister"),
                        map(TemplateId.T05, "Winterfell", "Stark"),
                        map(TemplateId.T06, "Harrenhal", "Blacks · Targaryen"),
                        map(TemplateId.T07, "Casterly Rock", "Lannister"),
                        map(TemplateId.T08, "Driftmark", "Velaryon"),
                        map(TemplateId.T09, "White Harbor", "Stark"),
                        map(TemplateId.T10, "Oldtown", "Greens · Targaryen"),
                        map(TemplateId.T11, "Dragonstone", "Blacks · Targaryen"),
                        map(TemplateId.T12, "King's Landing", "Greens · Targaryen", true),
                        map(TemplateId.T13, "Riverrun", "Tully"),
                        map(TemplateId.T14, "High Tide", "Velaryon")
                ),
                clanScores(
                        "Blacks · Targaryen", 10,
                        "Greens · Targaryen", 8,
                        "Velaryon", 7,
                        "Stark", 5,
                        "Lannister", 4,
                        "Tully", 3
                )
        ));
        return Map.copyOf(result);
    }

    private static CampaignData build(Campaign campaign, List<Mapping> mappings, Map<String, Integer> clanScores) {
        var cards = mappings.stream().map(mapping -> {
            Template template = TEMPLATES.get(mapping.template());
            return new StrongholdCard(
                    mapping.template().name(), mapping.name(), mapping.clan(), template.points(), template.lines(), mapping.kingsLanding()
            );
        }).toList();
        var data = new CampaignData(campaign, cards, clanScores);
        validate(data);
        return data;
    }

    private static void validate(CampaignData data) {
        if (data.strongholds().size() != 14) throw new IllegalStateException("campaign must contain 14 strongholds");
        if (data.strongholds().stream().map(StrongholdCard::id).distinct().count() != 14) {
            throw new IllegalStateException("campaign templates must be unique");
        }
        if (data.clanScores().size() != 6) throw new IllegalStateException("campaign must contain six clans");
        if (data.strongholds().stream().mapToInt(StrongholdCard::points).sum() != 29) {
            throw new IllegalStateException("campaign stronghold points must total 29");
        }
        if (data.clanScores().values().stream().mapToInt(Integer::intValue).sum() != 37) {
            throw new IllegalStateException("campaign clan points must total 37");
        }
        for (String clan : data.clanScores().keySet()) {
            if (data.clanStrongholds(clan).isEmpty()) throw new IllegalStateException("clan has no strongholds: " + clan);
        }
    }

    private static Template template(int points, BattleLine... lines) {
        return new Template(points, List.of(lines));
    }

    private static BattleLine military(String id, int threshold) { return new BattleLine.Military(id, threshold); }
    private static BattleLine symbols(String id, DieFace... faces) { return new BattleLine.Symbols(id, List.of(faces)); }
    private static Mapping map(TemplateId id, String name, String clan) { return map(id, name, clan, false); }
    private static Mapping map(TemplateId id, String name, String clan, boolean kingsLanding) {
        return new Mapping(id, name, clan, kingsLanding);
    }

    private static Map<String, Integer> clanScores(Object... values) {
        if (values.length % 2 != 0) throw new IllegalArgumentException("clan score pairs required");
        var result = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], (Integer) values[index + 1]);
        }
        return Map.copyOf(result);
    }
}
