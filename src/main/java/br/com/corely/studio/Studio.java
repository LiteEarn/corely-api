package br.com.corely.studio;

import br.com.corely.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "studios")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Studio extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
