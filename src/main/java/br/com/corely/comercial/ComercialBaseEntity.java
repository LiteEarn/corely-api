package br.com.corely.comercial;

import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.studio.Studio;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

@MappedSuperclass
@FilterDef(name = "comercialTenantFilter", parameters = @ParamDef(name = "studioId", type = UUID.class))
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public abstract class ComercialBaseEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    protected ComercialBaseEntity() {
    }

    public Studio getStudio() {
        return studio;
    }

    public void setStudio(Studio studio) {
        this.studio = studio;
    }
}
