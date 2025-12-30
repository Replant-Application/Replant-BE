package com.app.replant.domain.mission.enums;

/**
 * 지역 타입 - 대한민국 기초자치단체
 */
public enum RegionType {
    // ===== 서울특별시 =====
    SEOUL_JONGNO,       // 종로구
    SEOUL_JUNG,         // 중구
    SEOUL_YONGSAN,      // 용산구
    SEOUL_SEONGDONG,    // 성동구
    SEOUL_GWANGJIN,     // 광진구
    SEOUL_DONGDAEMUN,   // 동대문구
    SEOUL_JUNGNANG,     // 중랑구
    SEOUL_SEONGBUK,     // 성북구
    SEOUL_GANGBUK,      // 강북구
    SEOUL_DOBONG,       // 도봉구
    SEOUL_NOWON,        // 노원구
    SEOUL_EUNPYEONG,    // 은평구
    SEOUL_SEODAEMUN,    // 서대문구
    SEOUL_MAPO,         // 마포구
    SEOUL_YANGCHEON,    // 양천구
    SEOUL_GANGSEO,      // 강서구
    SEOUL_GURO,         // 구로구
    SEOUL_GEUMCHEON,    // 금천구
    SEOUL_YEONGDEUNGPO, // 영등포구
    SEOUL_DONGJAK,      // 동작구
    SEOUL_GWANAK,       // 관악구
    SEOUL_SEOCHO,       // 서초구
    SEOUL_GANGNAM,      // 강남구
    SEOUL_SONGPA,       // 송파구
    SEOUL_GANGDONG,     // 강동구

    // ===== 부산광역시 =====
    BUSAN_JUNG,         // 중구
    BUSAN_SEO,          // 서구
    BUSAN_DONG,         // 동구
    BUSAN_YEONGDO,      // 영도구
    BUSAN_BUSANJIN,     // 부산진구
    BUSAN_DONGNAE,      // 동래구
    BUSAN_NAM,          // 남구
    BUSAN_BUK,          // 북구
    BUSAN_HAEUNDAE,     // 해운대구
    BUSAN_SAHA,         // 사하구
    BUSAN_GEUMJEONG,    // 금정구
    BUSAN_GANGSEO,      // 강서구
    BUSAN_YEONJE,       // 연제구
    BUSAN_SUYEONG,      // 수영구
    BUSAN_SASANG,       // 사상구
    BUSAN_GIJANG,       // 기장군

    // ===== 대구광역시 =====
    DAEGU_JUNG,         // 중구
    DAEGU_DONG,         // 동구
    DAEGU_SEO,          // 서구
    DAEGU_NAM,          // 남구
    DAEGU_BUK,          // 북구
    DAEGU_SUSEONG,      // 수성구
    DAEGU_DALSEO,       // 달서구
    DAEGU_DALSEONG,     // 달성군
    DAEGU_GUNWI,        // 군위군

    // ===== 인천광역시 =====
    INCHEON_JUNG,       // 중구
    INCHEON_DONG,       // 동구
    INCHEON_MICHUHOL,   // 미추홀구
    INCHEON_YEONSU,     // 연수구
    INCHEON_NAMDONG,    // 남동구
    INCHEON_BUPYEONG,   // 부평구
    INCHEON_GYEYANG,    // 계양구
    INCHEON_SEO,        // 서구
    INCHEON_GANGHWA,    // 강화군
    INCHEON_ONGJIN,     // 옹진군

    // ===== 광주광역시 =====
    GWANGJU_DONG,       // 동구
    GWANGJU_SEO,        // 서구
    GWANGJU_NAM,        // 남구
    GWANGJU_BUK,        // 북구
    GWANGJU_GWANGSAN,   // 광산구

    // ===== 대전광역시 =====
    DAEJEON_DONG,       // 동구
    DAEJEON_JUNG,       // 중구
    DAEJEON_SEO,        // 서구
    DAEJEON_YUSEONG,    // 유성구
    DAEJEON_DAEDEOK,    // 대덕구

    // ===== 울산광역시 =====
    ULSAN_JUNG,         // 중구
    ULSAN_NAM,          // 남구
    ULSAN_DONG,         // 동구
    ULSAN_BUK,          // 북구
    ULSAN_ULJU,         // 울주군

    // ===== 세종특별자치시 =====
    SEJONG,             // 세종시

    // ===== 경기도 =====
    GYEONGGI_SUWON,     // 수원시
    GYEONGGI_SEONGNAM,  // 성남시
    GYEONGGI_UIJEONGBU, // 의정부시
    GYEONGGI_ANYANG,    // 안양시
    GYEONGGI_BUCHEON,   // 부천시
    GYEONGGI_GWANGMYEONG,// 광명시
    GYEONGGI_PYEONGTAEK,// 평택시
    GYEONGGI_DONGDUCHEON,// 동두천시
    GYEONGGI_ANSAN,     // 안산시
    GYEONGGI_GOYANG,    // 고양시
    GYEONGGI_GWACHEON,  // 과천시
    GYEONGGI_GURI,      // 구리시
    GYEONGGI_NAMYANGJU, // 남양주시
    GYEONGGI_OSAN,      // 오산시
    GYEONGGI_SIHEUNG,   // 시흥시
    GYEONGGI_GUNPO,     // 군포시
    GYEONGGI_UIWANG,    // 의왕시
    GYEONGGI_HANAM,     // 하남시
    GYEONGGI_YONGIN,    // 용인시
    GYEONGGI_PAJU,      // 파주시
    GYEONGGI_ICHEON,    // 이천시
    GYEONGGI_ANSEONG,   // 안성시
    GYEONGGI_GIMPO,     // 김포시
    GYEONGGI_HWASEONG,  // 화성시
    GYEONGGI_GWANGJU,   // 광주시
    GYEONGGI_YANGJU,    // 양주시
    GYEONGGI_POCHEON,   // 포천시
    GYEONGGI_YEOJU,     // 여주시
    GYEONGGI_YEONCHEON, // 연천군
    GYEONGGI_GAPYEONG,  // 가평군
    GYEONGGI_YANGPYEONG,// 양평군

    // ===== 강원특별자치도 =====
    GANGWON_CHUNCHEON,  // 춘천시
    GANGWON_WONJU,      // 원주시
    GANGWON_GANGNEUNG,  // 강릉시
    GANGWON_DONGHAE,    // 동해시
    GANGWON_TAEBAEK,    // 태백시
    GANGWON_SOKCHO,     // 속초시
    GANGWON_SAMCHEOK,   // 삼척시
    GANGWON_HONGCHEON,  // 홍천군
    GANGWON_HOENGSEONG, // 횡성군
    GANGWON_YEONGWOL,   // 영월군
    GANGWON_PYEONGCHANG,// 평창군
    GANGWON_JEONGSEON,  // 정선군
    GANGWON_CHEORWON,   // 철원군
    GANGWON_HWACHEON,   // 화천군
    GANGWON_YANGGU,     // 양구군
    GANGWON_INJE,       // 인제군
    GANGWON_GOSEONG,    // 고성군
    GANGWON_YANGYANG,   // 양양군

    // ===== 충청북도 =====
    CHUNGBUK_CHEONGJU,  // 청주시
    CHUNGBUK_CHUNGJU,   // 충주시
    CHUNGBUK_JECHEON,   // 제천시
    CHUNGBUK_BOEUN,     // 보은군
    CHUNGBUK_OKCHEON,   // 옥천군
    CHUNGBUK_YEONGDONG, // 영동군
    CHUNGBUK_JEUNGPYEONG,// 증평군
    CHUNGBUK_JINCHEON,  // 진천군
    CHUNGBUK_GOESAN,    // 괴산군
    CHUNGBUK_EUMSEONG,  // 음성군
    CHUNGBUK_DANYANG,   // 단양군

    // ===== 충청남도 =====
    CHUNGNAM_CHEONAN,   // 천안시
    CHUNGNAM_GONGJU,    // 공주시
    CHUNGNAM_BORYEONG,  // 보령시
    CHUNGNAM_ASAN,      // 아산시
    CHUNGNAM_SEOSAN,    // 서산시
    CHUNGNAM_NONSAN,    // 논산시
    CHUNGNAM_GYERYONG,  // 계룡시
    CHUNGNAM_DANGJIN,   // 당진시
    CHUNGNAM_GEUMSAN,   // 금산군
    CHUNGNAM_BUYEO,     // 부여군
    CHUNGNAM_SEOCHEON,  // 서천군
    CHUNGNAM_CHEONGYANG,// 청양군
    CHUNGNAM_HONGSEONG, // 홍성군
    CHUNGNAM_YESAN,     // 예산군
    CHUNGNAM_TAEAN,     // 태안군

    // ===== 전북특별자치도 =====
    JEONBUK_JEONJU,     // 전주시
    JEONBUK_GUNSAN,     // 군산시
    JEONBUK_IKSAN,      // 익산시
    JEONBUK_JEONGEUP,   // 정읍시
    JEONBUK_NAMWON,     // 남원시
    JEONBUK_GIMJE,      // 김제시
    JEONBUK_WANJU,      // 완주군
    JEONBUK_JINAN,      // 진안군
    JEONBUK_MUJU,       // 무주군
    JEONBUK_JANGSU,     // 장수군
    JEONBUK_IMSIL,      // 임실군
    JEONBUK_SUNCHANG,   // 순창군
    JEONBUK_GOCHANG,    // 고창군
    JEONBUK_BUAN,       // 부안군

    // ===== 전라남도 =====
    JEONNAM_MOKPO,      // 목포시
    JEONNAM_YEOSU,      // 여수시
    JEONNAM_SUNCHEON,   // 순천시
    JEONNAM_NAJU,       // 나주시
    JEONNAM_GWANGYANG,  // 광양시
    JEONNAM_DAMYANG,    // 담양군
    JEONNAM_GOKSEONG,   // 곡성군
    JEONNAM_GURYE,      // 구례군
    JEONNAM_GOHEUNG,    // 고흥군
    JEONNAM_BOSEONG,    // 보성군
    JEONNAM_HWASUN,     // 화순군
    JEONNAM_JANGHEUNG,  // 장흥군
    JEONNAM_GANGJIN,    // 강진군
    JEONNAM_HAENAM,     // 해남군
    JEONNAM_YEONGAM,    // 영암군
    JEONNAM_MUAN,       // 무안군
    JEONNAM_HAMPYEONG,  // 함평군
    JEONNAM_YEONGGWANG, // 영광군
    JEONNAM_JANGSEONG,  // 장성군
    JEONNAM_WANDO,      // 완도군
    JEONNAM_JINDO,      // 진도군
    JEONNAM_SINAN,      // 신안군

    // ===== 경상북도 =====
    GYEONGBUK_POHANG,   // 포항시
    GYEONGBUK_GYEONGJU, // 경주시
    GYEONGBUK_GIMCHEON, // 김천시
    GYEONGBUK_ANDONG,   // 안동시
    GYEONGBUK_GUMI,     // 구미시
    GYEONGBUK_YEONGJU,  // 영주시
    GYEONGBUK_YEONGCHEON,// 영천시
    GYEONGBUK_SANGJU,   // 상주시
    GYEONGBUK_MUNGYEONG,// 문경시
    GYEONGBUK_GYEONGSAN,// 경산시
    GYEONGBUK_UISEONG,  // 의성군
    GYEONGBUK_CHEONGSONG,// 청송군
    GYEONGBUK_YEONGYANG,// 영양군
    GYEONGBUK_YEONGDEOK,// 영덕군
    GYEONGBUK_CHEONGDO, // 청도군
    GYEONGBUK_GORYEONG, // 고령군
    GYEONGBUK_SEONGJU,  // 성주군
    GYEONGBUK_CHILGOK,  // 칠곡군
    GYEONGBUK_YECHEON,  // 예천군
    GYEONGBUK_BONGHWA,  // 봉화군
    GYEONGBUK_ULJIN,    // 울진군
    GYEONGBUK_ULLEUNG,  // 울릉군

    // ===== 경상남도 =====
    GYEONGNAM_CHANGWON, // 창원시
    GYEONGNAM_JINJU,    // 진주시
    GYEONGNAM_TONGYEONG,// 통영시
    GYEONGNAM_SACHEON,  // 사천시
    GYEONGNAM_GIMHAE,   // 김해시
    GYEONGNAM_MIRYANG,  // 밀양시
    GYEONGNAM_GEOJE,    // 거제시
    GYEONGNAM_YANGSAN,  // 양산시
    GYEONGNAM_UIRYEONG, // 의령군
    GYEONGNAM_HAMAN,    // 함안군
    GYEONGNAM_CHANGNYEONG,// 창녕군
    GYEONGNAM_GOSEONG,  // 고성군
    GYEONGNAM_NAMHAE,   // 남해군
    GYEONGNAM_HADONG,   // 하동군
    GYEONGNAM_SANCHEONG,// 산청군
    GYEONGNAM_HAMYANG,  // 함양군
    GYEONGNAM_GEOCHANG, // 거창군
    GYEONGNAM_HAPCHEON, // 합천군

    // ===== 제주특별자치도 =====
    JEJU_JEJU,          // 제주시
    JEJU_SEOGWIPO,      // 서귀포시

    // ===== 전국 =====
    ALL                 // 전국 (지역 무관)
}
