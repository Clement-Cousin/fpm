package com.mappy.fpm.batches.tomtom.dbf.timedomains;

import org.junit.Test;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class TimeDomainsParserTest {

    private final TimeDomainsParser parser = new TimeDomainsParser();

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_time_domain_could_not_be_parsed() {
        parser.parse(newHashSet(new TimeDomains(144, "error")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_time_domain_is_invalid() {
        parser.parse(newHashSet(new TimeDomains(144, "[{h11}(h2)]")));
    }

    @Test
    public void should_translate_tomtom_time_domain_collection_to_osm_opening_hours_string(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(h6){h2}]");
        TimeDomains tomtomTimesDomains2 = new TimeDomains(14420000000590L, "[(h12)(h22)]");

        String openingHours = parser.parse(newHashSet(tomtomTimesDomains, tomtomTimesDomains2));

        assertThat(openingHours).isEqualTo("1970-9999; 06:00-08:00 off, 12:00-22:00 off");
    }


    // INTERVAL

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_time_interval_mode_could_not_be_parsed() {
        parser.parse(newHashSet(new TimeDomains(144, "[(Z11)(Q23)]")));
    }

    @Test
    public void should_translate_month_interval(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(M3)(M5)]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Mar-May off");
    }


    // DURATION

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_time_duration_mode_could_not_be_parsed() {
        parser.parse(newHashSet(new TimeDomains(144, "[(Z11){Q23}]")));
    }

    @Test
    public void should_ignore_when_time_duration_has_z_mode() {

        String openingHours = parser.parse(newHashSet(new TimeDomains(144, "[(z37){z87}]")));

        assertThat(openingHours).isEqualTo("");
    }

    @Test
    public void should_return_empty_string_when_time_duration_has_f_mode() {

        String openingHours = parser.parse(newHashSet(new TimeDomains(144, "[(f12){d1}]")));

        assertThat(openingHours).isEqualTo("");
    }

    @Test
    public void should_return_empty_string_when_time_duration_has_l_mode() {

        String openingHours = parser.parse(newHashSet(new TimeDomains(144, "[(l12){d1}]")));

        assertThat(openingHours).isEqualTo("");
    }

    @Test
    public void should_return_empty_string_when_time_duration_has_multiple_compound() {

        String openingHours = parser.parse(newHashSet(new TimeDomains(144, "[[(h12){m30}]*[(d1){d15}]*[(M7){M1}]]")));

        assertThat(openingHours).isEqualTo("");
    }

    @Test
    public void should_return_empty_string_when_time_duration_has_plus_sign() {

        String openingHours = parser.parse(newHashSet(new TimeDomains(144, "[[(d12){d1}] + [(d13){d1}]]")));

        assertThat(openingHours).isEqualTo("");
    }

    @Test
    public void should_translate_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(h6){h2}]");
        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; 06:00-08:00 off");
    }

    @Test
    public void should_translate_hour_duration_over_midnight(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(h22){h8}]");
        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; 22:00-06:00 off");
    }

    @Test
    public void should_translate_hour_and_minutes_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(h14m15){h1m15}])]");
        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; 14:15-15:30 off");
    }

    @Test
    public void should_translate_month_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(M3){M5}]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Mar-Aug off");
    }

    @Test
    public void should_translate_month_duration_over_new_year(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(M10){M5}]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Oct-Mar off");
    }

    @Test
    public void should_translate_weekday_with_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(t1){h1}]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Su 00:00-01:00 off");
    }

    @Test
    public void should_translate_weekday_and_hour_with_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(t2h5){h1}]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Mo 05:00-06:00 off");
    }

    @Test
    public void should_translate_weekday_hour_and_minutes_with_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(t2h6m30){h6m30}])]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Mo 06:30-13:00 off");
    }

    @Test
    public void should_translate_multiple_weekday_and_hour_with_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[(t2t6){h10}]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

         assertThat(osmTimeDomain).isEqualTo("1970-9999; Mo,Fr 00:00-10:00 off");
    }

    @Test
    public void should_translate_multiple_months_and_hours_with_month_and_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[[(h11m30){h1}]*[(M11){M1}]]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Nov-Dec 11:30-12:30 off");
    }

    @Test
    public void should_translate_multiple_months_hour_and_weekdays_with_month_and_hour_duration(){
        TimeDomains tomtomTimesDomains = new TimeDomains(14420000000590L, "[[(t2t3t4t5t6h7){h3}]*[(M11){M5}]]");

        String osmTimeDomain = parser.parse(newHashSet(tomtomTimesDomains));

        assertThat(osmTimeDomain).isEqualTo("1970-9999; Nov-Apr Mo,Tu,We,Th,Fr 07:00-10:00 off");
    }
}