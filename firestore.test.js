const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    sourceUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    videoId: "dQw4w9WgXcQ",
    sourceTitle: "3-Hour Founder Podcast Livestream",
    channelName: "CreatorLab",
    thumbnailUrl: "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    durationSeconds: 1800,
    transcriptText: "We scaled from 0 to 100k users in 60 days using short form clips.",
    targetPlatform: "SHORTS",
    clipCount: 3,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validClipPayload(uid, projectId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    projectId: projectId,
    hookTitle: "The 1 Mistake That Kills 90% of Shorts",
    startTimeSec: 120,
    endTimeSec: 165,
    viralScore: 94,
    hookScore: 96,
    retentionScore: 91,
    shareabilityScore: 95,
    whyViral: "Strong contrarian hook in first 2 seconds with clear takeaway.",
    viralCaption: "Stop making this editing mistake on your Reels. Save this for later!",
    hashtags: ["#shorts", "#contentcreator", "#viraltips"],
    thumbnailText: "STOP DOING THIS",
    pinnedComment: "Which hook style works best for your channel?",
    captionStyle: "HORMOZI_NEON",
    aspectRatio: "RATIO_9_16",
    captionSegments: [
      {
        startOffsetSec: 0,
        endOffsetSec: 3,
        text: "Stop making this huge mistake",
        highlightWord: "mistake",
      },
    ],
    favorite: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write projects, clips, or drafts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/projects`)
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/hookDrafts`).get()
  );
});

test("Owner: can create, list, update, and delete StreamProject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1");

  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertSucceeds(
    projectRef.update({
      sourceTitle: "Updated Stream Title",
      clipCount: 4,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation & ghost field injection
  await assertFails(
    projectRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/projects`)
      .doc("proj_ghost")
      .set(validProjectPayload(ALICE_UID, { id: "proj_ghost" }))
  );
  await assertSucceeds(projectRef.delete());
});

test("Owner: can create ViralClip under existing project, update captions/hashtags, and reject orphan clips", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1");
  const clipRef = projectRef.collection("clips").doc("clip_1");
  const orphanClipRef = aliceDb
    .collection(`users/${ALICE_UID}/projects/nonexistent_proj/clips`)
    .doc("clip_orphan");

  // Orphan clip before parent exists must fail
  await assertFails(orphanClipRef.set(validClipPayload(ALICE_UID, "nonexistent_proj")));

  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(clipRef.set(validClipPayload(ALICE_UID, "proj_1")));
  await assertSucceeds(projectRef.collection("clips").get());

  // Partial update matching Kotlin ClipRepository.updateClip(...)
  await assertSucceeds(
    clipRef.update({
      hookTitle: "Why 99% of Creators Fail at Hooks",
      captionStyle: "CYBER_PULSE",
      favorite: true,
      hashtags: ["#youtubeshorts", "#editing", "#viral"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid endTimeSec <= startTimeSec
  await assertFails(
    clipRef.update({
      startTimeSec: 200,
      endTimeSec: 150,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    clipRef.update({
      hookTitle: "Future Timestamp Tamper",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's projects, clips, or drafts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.doc(`users/${ALICE_UID}/projects/proj_1`).set(validProjectPayload(ALICE_UID));
    await db
      .doc(`users/${ALICE_UID}/projects/proj_1/clips/clip_1`)
      .set(validClipPayload(ALICE_UID, "proj_1"));
    await db.doc(`users/${ALICE_UID}/hookDrafts/draft_1`).set({
      userId: ALICE_UID,
      topic: "Podcast opener",
      tone: "Bold",
      generatedHooks: "Hook 1: Nobody tells you this...",
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/projects/proj_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/projects/proj_1/clips/clip_1`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/projects/proj_1/clips/clip_1`).update({
      hookTitle: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/hookDrafts`).get());
});

test("Owner: can create, list, and delete HookLabDraft", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const draftRef = aliceDb.collection(`users/${ALICE_UID}/hookDrafts`).doc("draft_1");
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    draftRef.set({
      userId: ALICE_UID,
      topic: "How I gained 50k subscribers from live streams",
      tone: "Contrarian",
      generatedHooks: "1. Stop uploading raw VODs...",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/hookDrafts`).get());
  await assertSucceeds(draftRef.delete());
});
