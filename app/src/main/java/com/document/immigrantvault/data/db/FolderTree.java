package com.document.immigrantvault.data.db;

import com.document.immigrantvault.data.db.entity.VaultFolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers for the person → folder → nested-folder tree used by the Files tab.
 */
public final class FolderTree {

    private FolderTree() {
    }

    public static List<VaultFolder> childrenOf(List<VaultFolder> all, Long parentId) {
        List<VaultFolder> children = new ArrayList<>();
        if (all == null) {
            return children;
        }
        for (VaultFolder folder : all) {
            if (parentId == null) {
                if (folder.parentFolderId == null) {
                    children.add(folder);
                }
            } else if (parentId.equals(folder.parentFolderId)) {
                children.add(folder);
            }
        }
        return children;
    }

    /** This folder's id plus every nested folder beneath it. */
    public static List<Long> inclusiveIds(long rootId, List<VaultFolder> all) {
        List<Long> ids = new ArrayList<>();
        collectIds(rootId, all, ids);
        return ids;
    }

    private static void collectIds(long folderId, List<VaultFolder> all, List<Long> ids) {
        ids.add(folderId);
        if (all == null) {
            return;
        }
        for (VaultFolder folder : all) {
            if (folder.parentFolderId != null && folder.parentFolderId == folderId) {
                collectIds(folder.id, all, ids);
            }
        }
    }

    public static int inclusiveFileCount(long folderId, List<VaultFolder> all,
                                         Map<Long, Integer> directCounts) {
        int count = 0;
        if (directCounts != null && directCounts.containsKey(folderId)) {
            count += directCounts.get(folderId);
        }
        if (all == null) {
            return count;
        }
        for (VaultFolder folder : all) {
            if (folder.parentFolderId != null && folder.parentFolderId == folderId) {
                count += inclusiveFileCount(folder.id, all, directCounts);
            }
        }
        return count;
    }

    public static String path(VaultFolder folder, List<VaultFolder> all) {
        if (folder == null) {
            return "";
        }
        Map<Long, VaultFolder> byId = index(all);
        List<String> parts = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        VaultFolder current = folder;
        while (current != null && seen.add(current.id)) {
            parts.add(0, current.name != null ? current.name : "");
            if (current.parentFolderId == null) {
                break;
            }
            current = byId.get(current.parentFolderId);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(" / ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    /**
     * Parents before children so a restore can insert nested folders without
     * tripping the self-foreign-key.
     */
    public static List<VaultFolder> parentsFirst(List<VaultFolder> folders) {
        List<VaultFolder> ordered = new ArrayList<>();
        if (folders == null || folders.isEmpty()) {
            return ordered;
        }
        Map<Long, VaultFolder> byId = index(folders);
        Set<Long> seen = new HashSet<>();
        for (VaultFolder folder : folders) {
            visit(folder, byId, seen, ordered);
        }
        return ordered;
    }

    public static List<VaultFolder> sanitizeParents(List<VaultFolder> folders) {
        if (folders == null) {
            return new ArrayList<>();
        }
        Set<Long> ids = new HashSet<>();
        for (VaultFolder folder : folders) {
            ids.add(folder.id);
        }
        for (VaultFolder folder : folders) {
            if (folder.parentFolderId != null && !ids.contains(folder.parentFolderId)) {
                folder.parentFolderId = null;
            }
        }
        return folders;
    }

    private static void visit(VaultFolder folder, Map<Long, VaultFolder> byId,
                              Set<Long> seen, List<VaultFolder> ordered) {
        if (folder == null || seen.contains(folder.id)) {
            return;
        }
        if (folder.parentFolderId != null) {
            visit(byId.get(folder.parentFolderId), byId, seen, ordered);
        }
        seen.add(folder.id);
        ordered.add(folder);
    }

    private static Map<Long, VaultFolder> index(List<VaultFolder> folders) {
        Map<Long, VaultFolder> byId = new HashMap<>();
        if (folders == null) {
            return byId;
        }
        for (VaultFolder folder : folders) {
            byId.put(folder.id, folder);
        }
        return byId;
    }
}
