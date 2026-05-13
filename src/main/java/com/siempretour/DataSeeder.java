package com.siempretour;

import com.siempretour.Tours.Models.*;
import com.siempretour.Tours.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TourRepository tourRepository;

    @Override
    public void run(String... args) {
        if (tourRepository.count() > 0) {
            log.info("Database already has tours, skipping seed.");
            return;
        }

        log.info("Seeding test tours...");
        List<Tour> tours = new ArrayList<>();

        // ========== TURKISH TOURS ==========

        // TR Tour 1: Italy-Slovenia-Austria
        Tour tr1 = new Tour();
        tr1.setName("İtalya - Slovenya - Avusturya Turu");
        tr1.setSlug("italya-slovenya-avusturya-turu");
        tr1.setLanguage("tr");
        tr1.setDestination("Italy");
        tr1.setCategory(TourCategory.CULTURE);
        tr1.setStatus(TourStatus.PUBLISHED);
        tr1.setIsActive(true);
        tr1.setPrice(new BigDecimal("2500"));
        tr1.setDuration(5);
        tr1.setMinParticipants(1);
        tr1.setMaxParticipants(30);
        tr1.setGeneralInfo("İtalya Yarımadasındaki insan varlığının izleri bu İtalik kavimlerin yarımadaya ulaşmalarından çok öncelerine, 200 binyıl öncesi Yeni Taş Çağı'na kadar dayanır. İnsanlık tarihinin kültür başkentlerinden olan İtalya'yı Siempre Tur ile keşfedin.<br><br>Tur boyunca hem rehberlerimizin tarihi anlatımlarından hem de İtalya'nın tarih kokan doğal güzelliklerinden çok etkileneceksiniz.");
        tr1.setPlacesVisited("İtalya, Slovenya, Avusturya");
        tr1.setWhatExpect("<ul>\n<li>Havayolu ile ekonomi sınıfı uçak bileti</li>\n<li>Havalimanı vergileri</li>\n<li>Alan/otel/alan transferleri</li>\n<li>3-4 Yıldız otellerde oda+kahvaltı konaklama</li>\n<li>Türkçe tur liderliği hizmeti</li>\n</ul>");
        tr1.setMainPhoto("images/tour-photos/India/01/01.webp");
        tr1.setImage1("images/tour-photos/India/01/01.webp");
        tr1.setImage2("images/tour-photos/India/01/02.webp");
        tr1.setImage3("images/tour-photos/India/01/03.webp");
        tr1.setImage4("images/tour-photos/India/01/04.webp");
        tr1.setImage5("images/tour-photos/India/01/05.webp");
        tr1.setImage6("images/tour-photos/India/01/06.webp");
        tr1.setImagealt("İtalya Slovenya Avusturya Turu");
        tr1.setPersonNumber("18");
        tr1.setMinimumAge("18");

        // Day info
        TourDay tr1d1 = new TourDay();
        tr1d1.setDayNumber(1);
        tr1d1.setTitle("İSTANBUL – SALZBURG – HALLSTATT - SALZBURG");
        tr1d1.setDescription("İstanbul Havalimanı'ndan Salzburg'a uçuş. Hallstatt'a gidiyoruz. UNESCO dünya mirası listesindeki büyüleyici kasaba. Göl kenarında yürüyüş ve serbest zaman sonrası Salzburg şehir turuna başlıyoruz.");
        tr1d1.setTour(tr1);
        tr1.getDayInfo().add(tr1d1);

        TourDay tr1d2 = new TourDay();
        tr1d2.setDayNumber(2);
        tr1d2.setTitle("SALZBURG - BLED GÖLÜ – LJUBLJANA");
        tr1d2.setDescription("Avrupa'nın en güzel 10 kasabası listesine giren Bled gölüne hareket. Doğanın insanlığa armağanı Bled gölünde yürüyüş. Ardından Ljubljana şehir turu.");
        tr1d2.setTour(tr1);
        tr1.getDayInfo().add(tr1d2);

        TourDay tr1d3 = new TourDay();
        tr1d3.setDayNumber(3);
        tr1d3.setTitle("LJUBLJANA – VENEDİK - VERONA");
        tr1d3.setDescription("Venedik şehir turu: San Marco Meydanı, San Marco Bazilikası, Dükler Sarayı, Büyük Kanal, Rialto Köprüsü. Sonrasında Verona'ya hareket.");
        tr1d3.setTour(tr1);
        tr1.getDayInfo().add(tr1d3);

        TourDay tr1d4 = new TourDay();
        tr1d4.setDayNumber(4);
        tr1d4.setTitle("VERONA – GARDA – COMO – MİLANO");
        tr1d4.setDescription("Garda Gölü ve Como Gölü ziyareti. Milano'ya transfer ve konaklama.");
        tr1d4.setTour(tr1);
        tr1.getDayInfo().add(tr1d4);

        TourDay tr1d5 = new TourDay();
        tr1d5.setDayNumber(5);
        tr1d5.setTitle("MİLANO - İSTANBUL");
        tr1d5.setDescription("Milano şehir turu: Duomo, Galleria Vittorio Emanuele. Havalimanına transfer ve İstanbul'a dönüş uçuşu.");
        tr1d5.setTour(tr1);
        tr1.getDayInfo().add(tr1d5);

        // Route
        tr1.setRoute(List.of(
                new TourRouteStop("İstanbul", "Turkey"),
                new TourRouteStop("Salzburg", "Austria"),
                new TourRouteStop("Hallstatt", "Austria"),
                new TourRouteStop("Bled", "Slovenia"),
                new TourRouteStop("Ljubljana", "Slovenia"),
                new TourRouteStop("Venedik", "Italy"),
                new TourRouteStop("Verona", "Italy"),
                new TourRouteStop("Milano", "Italy")
        ));

        tr1.setRouteCoordinates(List.of(
                new TourRouteCoordinate("İstanbul", "Türkiye", 41.015137, 28.97953),
                new TourRouteCoordinate("Salzburg", "Avusturya", 47.799973, 13.045283),
                new TourRouteCoordinate("Hallstatt", "Avusturya", 47.56231, 13.648932),
                new TourRouteCoordinate("Bled", "Slovenya", 46.36842, 14.11006),
                new TourRouteCoordinate("Ljubljana", "Slovenya", 46.050027, 14.506929),
                new TourRouteCoordinate("Venedik", "İtalya", 45.440847, 12.315515),
                new TourRouteCoordinate("Verona", "İtalya", 45.438384, 10.991622),
                new TourRouteCoordinate("Milano", "İtalya", 45.464203, 9.189982)
        ));

        tours.add(tr1);

        // TR Tour 2: Roma - Napoli
        Tour tr2 = new Tour();
        tr2.setName("Roma ve Napoli Kültür Turu");
        tr2.setSlug("roma-napoli-kultur-turu");
        tr2.setLanguage("tr");
        tr2.setDestination("Italy");
        tr2.setCategory(TourCategory.HISTORICAL);
        tr2.setStatus(TourStatus.PUBLISHED);
        tr2.setIsActive(true);
        tr2.setPrice(new BigDecimal("3200"));
        tr2.setDuration(7);
        tr2.setMinParticipants(1);
        tr2.setMaxParticipants(25);
        tr2.setGeneralInfo("Roma'nın antik sokaklarında tarihe yolculuk yapın, Napoli'nin eşsiz mutfağını keşfedin. Colosseum'dan Pompeii'ye, Vatikan'dan Amalfi Sahili'ne uzanan benzersiz bir kültür turu.<br><br>Siempre Tour deneyimli rehber ekibi eşliğinde İtalya'nın en etkileyici iki şehrini keşfedeceksiniz.");
        tr2.setPlacesVisited("Roma, Napoli, Pompeii, Amalfi");
        tr2.setWhatExpect("<ul>\n<li>Uçak bileti ve havalimanı transferleri</li>\n<li>4 Yıldız otellerde konaklama</li>\n<li>Profesyonel Türkçe rehberlik</li>\n<li>Müze giriş ücretleri</li>\n</ul>");
        tr2.setMainPhoto("images/tour-photos/India/01/02.webp");
        tr2.setImage1("images/tour-photos/India/01/02.webp");
        tr2.setImage2("images/tour-photos/India/01/03.webp");
        tr2.setImage3("images/tour-photos/India/01/04.webp");
        tr2.setImage4("images/tour-photos/India/01/05.webp");
        tr2.setImage5("images/tour-photos/India/01/06.webp");
        tr2.setImage6("images/tour-photos/India/01/01.webp");
        tr2.setImagealt("Roma Napoli Kültür Turu");

        TourDay tr2d1 = new TourDay();
        tr2d1.setDayNumber(1);
        tr2d1.setTitle("İSTANBUL – ROMA");
        tr2d1.setDescription("İstanbul'dan Roma'ya uçuş. Otele transfer ve serbest zaman.");
        tr2d1.setTour(tr2);
        tr2.getDayInfo().add(tr2d1);

        TourDay tr2d2 = new TourDay();
        tr2d2.setDayNumber(2);
        tr2d2.setTitle("ROMA ŞEHİR TURU");
        tr2d2.setDescription("Colosseum, Roma Forumu, Trevi Çeşmesi, İspanyol Merdivenleri, Pantheon ziyareti.");
        tr2d2.setTour(tr2);
        tr2.getDayInfo().add(tr2d2);

        TourDay tr2d3 = new TourDay();
        tr2d3.setDayNumber(3);
        tr2d3.setTitle("VATİKAN");
        tr2d3.setDescription("Vatikan Müzesi, Sistine Şapeli, San Pietro Bazilikası ziyareti.");
        tr2d3.setTour(tr2);
        tr2.getDayInfo().add(tr2d3);

        tr2.setRoute(List.of(
                new TourRouteStop("İstanbul", "Turkey"),
                new TourRouteStop("Roma", "Italy"),
                new TourRouteStop("Napoli", "Italy"),
                new TourRouteStop("Pompeii", "Italy"),
                new TourRouteStop("Amalfi", "Italy")
        ));

        tr2.setRouteCoordinates(List.of(
                new TourRouteCoordinate("İstanbul", "Türkiye", 41.015137, 28.97953),
                new TourRouteCoordinate("Roma", "İtalya", 41.902782, 12.496366),
                new TourRouteCoordinate("Napoli", "İtalya", 40.851775, 14.268124),
                new TourRouteCoordinate("Pompeii", "İtalya", 40.750638, 14.486865),
                new TourRouteCoordinate("Amalfi", "İtalya", 40.634006, 14.602676)
        ));

        tours.add(tr2);

        // ========== ENGLISH TOURS ==========

        // EN Tour 1: Piedmont Holiday
        Tour en1 = new Tour();
        en1.setName("Piedmont holiday in Italy, food, wine & culture");
        en1.setSlug("piedmont-holiday-in-italy-food-wine-culture");
        en1.setLanguage("en");
        en1.setDestination("Italy");
        en1.setCategory(TourCategory.FOOD_AND_WINE);
        en1.setStatus(TourStatus.PUBLISHED);
        en1.setIsActive(true);
        en1.setPrice(new BigDecimal("2942"));
        en1.setDuration(8);
        en1.setMinParticipants(1);
        en1.setMaxParticipants(20);
        en1.setGeneralInfo("Discover the landscapes of the Langhe region of northern Italy set between the Alps and the Apennine mountains, famous for the rolling vineyards and the Barolo wine. This land is blessed with rich and fertile lands that produce extraordinary specialties including gourmet cheeses, delicious pastries and hazelnut chocolates.");
        en1.setPlacesVisited("Barolo, Novello, Monforte, Neive, Barbaresco, Alba, Turin");
        en1.setWhatExpect("<ul>\n<li>7 nights accommodation in charming hotels</li>\n<li>Daily breakfast</li>\n<li>Guided wine tastings</li>\n<li>Cooking class</li>\n<li>All transfers</li>\n</ul>");
        en1.setMainPhoto("images/tour-photos/India/01/01.webp");
        en1.setImage1("images/tour-photos/India/01/01.webp");
        en1.setImage2("images/tour-photos/India/01/02.webp");
        en1.setImage3("images/tour-photos/India/01/03.webp");
        en1.setImage4("images/tour-photos/India/01/04.webp");
        en1.setImage5("images/tour-photos/India/01/05.webp");
        en1.setImage6("images/tour-photos/India/01/06.webp");
        en1.setImagealt("Piedmont Italy Food Wine Tour");

        TourDay en1d1 = new TourDay();
        en1d1.setDayNumber(1);
        en1d1.setTitle("Arrival in Barolo");
        en1d1.setDescription("Meeting with your guide at 6pm at the hotel for the general briefing. Dinner and overnight stay in Barolo.");
        en1d1.setTour(en1);
        en1.getDayInfo().add(en1d1);

        TourDay en1d2 = new TourDay();
        en1d2.setDayNumber(2);
        en1d2.setTitle("Barolo to Monforte");
        en1d2.setDescription("From Barolo descending gently to Talloria Valley we will reach Novello, a belvedere town before continuing through panoramic fields. The hike will end in the beautiful town of Monforte. Visit a local winery and lunch.");
        en1d2.setTour(en1);
        en1.getDayInfo().add(en1d2);

        TourDay en1d3 = new TourDay();
        en1d3.setDayNumber(3);
        en1d3.setTitle("Cooking Class");
        en1d3.setDescription("After breakfast a soft hike will take you to the quiet village of Castiglione Falletto. Private transfer to a local Agriturismo where you will enjoy a cooking class!");
        en1d3.setTour(en1);
        en1.getDayInfo().add(en1d3);

        en1.setRoute(List.of(
                new TourRouteStop("Barolo", "Italy"),
                new TourRouteStop("Novello", "Italy"),
                new TourRouteStop("Monforte", "Italy"),
                new TourRouteStop("Neive", "Italy"),
                new TourRouteStop("Alba", "Italy"),
                new TourRouteStop("Turin", "Italy")
        ));

        en1.setRouteCoordinates(List.of(
                new TourRouteCoordinate("Barolo", "Italy", 44.609, 7.943),
                new TourRouteCoordinate("Novello", "Italy", 44.584, 7.929),
                new TourRouteCoordinate("Monforte", "Italy", 44.581, 7.965),
                new TourRouteCoordinate("Alba", "Italy", 44.700, 8.035),
                new TourRouteCoordinate("Turin", "Italy", 45.070, 7.687)
        ));

        tours.add(en1);

        // EN Tour 2: Umbria Family Holiday
        Tour en2 = new Tour();
        en2.setName("Umbria Family Holiday Italy");
        en2.setSlug("umbria-family-holiday-italy");
        en2.setLanguage("en");
        en2.setDestination("Italy");
        en2.setCategory(TourCategory.FAMILY);
        en2.setStatus(TourStatus.PUBLISHED);
        en2.setIsActive(true);
        en2.setPrice(new BigDecimal("1850"));
        en2.setDuration(6);
        en2.setMinParticipants(1);
        en2.setMaxParticipants(15);
        en2.setGeneralInfo("Explore the green heart of Italy with your family. Umbria offers medieval hilltop towns, stunning countryside, and authentic Italian cuisine in a relaxed, family-friendly setting. Perfect for families looking for culture and adventure.");
        en2.setPlacesVisited("Perugia, Assisi, Spoleto, Orvieto, Todi");
        en2.setWhatExpect("<ul>\n<li>5 nights in family-friendly agriturismo</li>\n<li>Daily breakfast and 3 dinners</li>\n<li>Guided excursions</li>\n<li>Olive oil tasting</li>\n<li>All local transfers</li>\n</ul>");
        en2.setMainPhoto("images/tour-photos/India/01/03.webp");
        en2.setImage1("images/tour-photos/India/01/03.webp");
        en2.setImage2("images/tour-photos/India/01/04.webp");
        en2.setImage3("images/tour-photos/India/01/05.webp");
        en2.setImage4("images/tour-photos/India/01/06.webp");
        en2.setImage5("images/tour-photos/India/01/01.webp");
        en2.setImage6("images/tour-photos/India/01/02.webp");
        en2.setImagealt("Umbria Family Holiday Italy");

        TourDay en2d1 = new TourDay();
        en2d1.setDayNumber(1);
        en2d1.setTitle("Arrival in Perugia");
        en2d1.setDescription("Arrive in Perugia, the capital of Umbria. Transfer to your agriturismo and settle in. Welcome dinner with local specialties.");
        en2d1.setTour(en2);
        en2.getDayInfo().add(en2d1);

        TourDay en2d2 = new TourDay();
        en2d2.setDayNumber(2);
        en2d2.setTitle("Assisi Day Trip");
        en2d2.setDescription("Visit the stunning hilltop town of Assisi, home of St. Francis. Explore the Basilica, medieval streets, and enjoy panoramic views of the Umbrian valley.");
        en2d2.setTour(en2);
        en2.getDayInfo().add(en2d2);

        en2.setRoute(List.of(
                new TourRouteStop("Perugia", "Italy"),
                new TourRouteStop("Assisi", "Italy"),
                new TourRouteStop("Spoleto", "Italy"),
                new TourRouteStop("Orvieto", "Italy"),
                new TourRouteStop("Todi", "Italy")
        ));

        en2.setRouteCoordinates(List.of(
                new TourRouteCoordinate("Perugia", "Italy", 43.110, 12.389),
                new TourRouteCoordinate("Assisi", "Italy", 43.070, 12.619),
                new TourRouteCoordinate("Spoleto", "Italy", 42.726, 12.737),
                new TourRouteCoordinate("Orvieto", "Italy", 42.718, 12.110),
                new TourRouteCoordinate("Todi", "Italy", 42.781, 12.407)
        ));

        tours.add(en2);

        // Save all
        tourRepository.saveAll(tours);
        log.info("Seeded {} test tours (2 TR, 2 EN) for Italy.", tours.size());
    }
}
