package com.app.replant.controller.dto;


import com.app.replant.entity.Member;
import com.app.replant.entity.type.Authority;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private String memberId;
    private String name;
    private String phone;
    private String birthDate;
    private String birthBack;

    public static MemberDto fromEntity(Member member) {
        return MemberDto.builder()
                .memberId(member.getMemberId())
                .name(member.getMemberName())
                .phone(member.getPhone())
                .birthDate(member.getBirthDate())
                .birthBack(member.getBirthBack())
                .build();
    }
}