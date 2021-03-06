package com.limefriends.molde.menu_map.callbackMethod;


import com.limefriends.molde.menu_map.entity.MoldeSearchMapInfoEntity;

import java.io.IOException;

public interface MoldeMapInfoRecyclerViewAdapterCallback {
    void showToast(String toast);
    void applyMapInfo(MoldeSearchMapInfoEntity entity);
    void writeSearchMapHistory(MoldeSearchMapInfoEntity entity, String historyStr) throws IOException;
}
