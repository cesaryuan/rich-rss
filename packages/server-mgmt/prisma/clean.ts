import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log(`Clean database...`);

  await prisma.eventHook.deleteMany();
  await prisma.articleRef.deleteMany();
  await prisma.article.deleteMany();
  await prisma.subscription.deleteMany();
  await prisma.articleExporterTarget.deleteMany();
  await prisma.articleExporter.deleteMany();
  await prisma.bucket.deleteMany();
  await prisma.feed.deleteMany();
  await prisma.notebook.deleteMany();
  await prisma.stream.deleteMany();
  await prisma.noFollowUrl.deleteMany();
  await prisma.articlePostProcessor.deleteMany();
  await prisma.user.deleteMany();
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });