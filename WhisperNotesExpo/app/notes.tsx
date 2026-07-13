// notes.tsx — Kalıcı not arşivi
import { useEffect } from "react";
import {
  View, Text, TouchableOpacity, ScrollView, Share, Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useApp } from "../src/context/AppContext";
import { exportText, formatNoteDate, sourceLabel } from "../src/utils/export";
import type { SavedNote } from "../src/types/note";

function NoteCard({
  note,
  onOpen,
  onCopy,
  onShare,
  onDelete,
}: {
  note: SavedNote;
  onOpen: () => void;
  onCopy: () => void;
  onShare: () => void;
  onDelete: () => void;
}) {
  return (
    <TouchableOpacity
      onPress={onOpen}
      activeOpacity={0.8}
      className="rounded-2xl p-4 mb-3 border"
      style={{ backgroundColor: "#1C1B1F", borderColor: "#D0BCFF33" }}
    >
      <View className="flex-row items-center mb-2">
        <View className="flex-1">
          <Text className="text-sm font-bold text-white" numberOfLines={1}>
            {note.title || "İsimsiz Not"}
          </Text>
          <Text className="text-xs text-gray-500 mt-0.5">
            {formatNoteDate(note.createdAt)} • {sourceLabel(note.sourceType)} • {note.language.toUpperCase()}
          </Text>
        </View>
        <Text className="text-gray-500 ml-2">›</Text>
      </View>

      <Text
        className="text-xs mb-3"
        style={{ color: "#BDBDBD", lineHeight: 18 }}
        numberOfLines={3}
      >
        {note.fullText}
      </Text>

      <View className="flex-row items-center">
        <TouchableOpacity onPress={onCopy} className="flex-row items-center mr-4">
          <Text className="text-xs mr-1">📋</Text>
          <Text className="text-xs" style={{ color: "#D0BCFF" }}>Kopyala</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={onShare} className="flex-row items-center mr-4">
          <Text className="text-xs mr-1">📤</Text>
          <Text className="text-xs" style={{ color: "#D0BCFF" }}>Dışa aktar</Text>
        </TouchableOpacity>
        <View className="flex-1" />
        <TouchableOpacity onPress={onDelete} className="flex-row items-center">
          <Text className="text-xs mr-1">🗑️</Text>
          <Text className="text-xs" style={{ color: "#F28B82" }}>Sil</Text>
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );
}

export default function NotesScreen() {
  const { savedNotes, refreshNotes, deleteNote, openNote } = useApp();

  useEffect(() => {
    refreshNotes();
  }, [refreshNotes]);

  const handleCopy = async (note: SavedNote) => {
    await Clipboard.setStringAsync(exportText(note));
    Alert.alert("", "Not panoya kopyalandı ✓");
  };

  const handleShare = async (note: SavedNote) => {
    await Share.share({ message: exportText(note), title: note.title });
  };

  const handleDelete = (note: SavedNote) => {
    Alert.alert(
      "Notu sil",
      `"${note.title}" silinecek. Onaylıyor musun?`,
      [
        { text: "İptal", style: "cancel" },
        {
          text: "Sil",
          style: "destructive",
          onPress: () => deleteNote(note.id),
        },
      ]
    );
  };

  const handleOpen = (note: SavedNote) => {
    openNote(note);
    router.push("/result");
  };

  return (
    <SafeAreaView className="flex-1 bg-dark-bg">
      {/* Başlık */}
      <View className="flex-row items-center px-2 py-3" style={{ backgroundColor: "#1C1B1F" }}>
        <TouchableOpacity onPress={() => router.back()} className="p-2 mr-2">
          <Text className="text-xl" style={{ color: "#D0BCFF" }}>←</Text>
        </TouchableOpacity>
        <Text className="text-lg font-bold text-white">Notlarım</Text>
      </View>

      {savedNotes.length === 0 ? (
        <View className="flex-1 items-center justify-center px-8">
          <Text className="text-5xl mb-4">📝</Text>
          <Text className="text-base font-bold text-white mb-2">Henüz kayıtlı not yok</Text>
          <Text className="text-sm text-gray-400 text-center">
            Dosya yüklediğinde veya canlı kaydı not aldığında burada kalıcı olarak görünür.
          </Text>
        </View>
      ) : (
        <ScrollView className="flex-1 px-4 pt-4" showsVerticalScrollIndicator={false}>
          <Text
            className="text-xs font-bold mb-3"
            style={{ color: "#E8DEF8" }}
          >
            {savedNotes.length} kayıtlı not
          </Text>

          {savedNotes.map((note) => (
            <NoteCard
              key={note.id}
              note={note}
              onOpen={() => handleOpen(note)}
              onCopy={() => handleCopy(note)}
              onShare={() => handleShare(note)}
              onDelete={() => handleDelete(note)}
            />
          ))}

          <View className="h-8" />
        </ScrollView>
      )}
    </SafeAreaView>
  );
}
