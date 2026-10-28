package Windy.setting;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Setting {
    @Expose
    @SerializedName("name")
    public String name;
    private Predicate<Setting> parentCondition;


    @SuppressWarnings("unchecked")
    public <T extends Setting> Setting addParent(T parentSetting, Predicate<T> condition) {
        this.parentCondition = setting -> condition.test((T) parentSetting);
        return this;
    }

    public boolean isVisible() {
        if (parentCondition == null) {
            return true;
        }
        try {
            return parentCondition.test(this);
        } catch (ClassCastException e) {
            return true;
        }
    }

}
