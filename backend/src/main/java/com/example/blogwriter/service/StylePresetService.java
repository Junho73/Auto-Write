package com.example.blogwriter.service;

import com.example.blogwriter.model.StylePreset;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fixed set of structure/tone guides for the OpenAI prompt. These describe
 * *shape and voice*, never copied text from real posts — scraping real
 * developer blogs was explicitly ruled out (copyright/abuse risk).
 */
@Service
public class StylePresetService {

    private static final List<StylePreset> PRESETS = List.of(
        new StylePreset(
            "retrospective",
            "회고형",
            "완성된 아티클보다는 개발 일지(TIL)에 가까운 학습/프로젝트 기록. 화려한 배경 설명 없이 " +
            "'이번 ~에서 ~를 하며 ~해보았다' 식으로 바로 본론에 들어가고, 1인칭 반말 어조('~했다', " +
            "'~해보았다', '~인 것 같다')로 담백하게 쓴다. 소제목으로 섹션을 나누고, 여러 옵션/기능을 " +
            "비교하거나 구조화된 정보를 전달할 때는 마크다운 표를 적극 활용하며, 관련 코드가 있으면 " +
            "코드 블록으로 보여준다. 잘된 점과 아쉬운 점, 다음에 시도해볼 것으로 마무리한다."
        ),
        new StylePreset(
            "troubleshooting",
            "트러블슈팅형",
            "겪었던 문제 상황을 구체적으로 제시하고(에러 메시지나 증상 포함), 원인을 추적해나가는 과정을 단계별로 서술한 뒤, 최종 해결 방법과 얻은 교훈으로 마무리하는 구조."
        ),
        new StylePreset(
            "tutorial",
            "튜토리얼형",
            "독자가 처음부터 따라할 수 있도록 사전 준비물을 안내하고, 번호를 매긴 단계별 절차와 코드 예시를 순서대로 제시한 뒤, 마지막에 전체 요약과 다음에 시도해볼 만한 것을 제안하는 구조."
        ),
        new StylePreset(
            "explainer",
            "기술설명형",
            "하나의 개념이나 기술을 소개하며 왜 필요한지 배경을 먼저 설명하고, 핵심 동작 원리를 예시와 함께 풀어낸 뒤, 실무에서 언제 쓰면 좋은지 정리하는 구조."
        ),
        new StylePreset(
            "weekly_digest",
            "위클리 다이제스트형",
            "깊이 있는 기술 분석보다는 가볍고 편안한 어조로 '이번 주에 이런 소식들이 있었어요' 식으로 소개하는 " +
            "구조. 짧은 인사/도입부로 시작해서, 주어진 소식(주제)들을 하나씩 짧게(2~4문장) 무슨 내용이고 왜 " +
            "주목할 만한지 소개한 뒤, 가벼운 마무리 인사로 끝낸다. 여러 개의 주제가 주어지면 각각을 소제목으로 " +
            "구분해서 나열식으로 다룬다."
        )
    );

    private static final Map<String, StylePreset> BY_ID =
        PRESETS.stream().collect(Collectors.toMap(StylePreset::id, p -> p));

    public List<StylePreset> listPresets() {
        return PRESETS;
    }

    public StylePreset getOrDefault(String id) {
        return BY_ID.getOrDefault(id, PRESETS.get(0));
    }
}
